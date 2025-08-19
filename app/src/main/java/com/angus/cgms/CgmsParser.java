package com.angus.cgms;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CgmsParser {

    public static class CgmMeasurement {
        public int size;
        public int flags;
        public Float glucose;
        public Integer timeOffset;
        public Float trend;
        public Float quality;
        public byte[] sensorStatusAnnunciation;

        @Override public String toString() {
            return "size=" + size +
                    ", flags=0x" + Integer.toHexString(flags) +
                    ", glucose=" + (glucose == null ? "?" : glucose) +
                    ", timeOffset=" + (timeOffset == null ? "?" : timeOffset) +
                    (trend != null ? (", trend=" + trend) : "") +
                    (quality != null ? (", quality=" + quality) : "") +
                    (sensorStatusAnnunciation != null ? (", status=" + bytesToHex(sensorStatusAnnunciation)) : "");
        }

        private String bytesToHex(byte[] b) {
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02X ", x));
            return sb.toString().trim();
        }
    }

    public static CgmMeasurement parseMeasurement(byte[] v) {
        CgmMeasurement m = new CgmMeasurement();
        if (v == null || v.length < 4) return m;

        ByteBuffer buf = ByteBuffer.wrap(v).order(ByteOrder.LITTLE_ENDIAN);
        m.size  = (buf.get() & 0xFF);
        m.flags = (buf.get() & 0xFF);

        m.glucose = readSfloat(buf);
        m.timeOffset = (buf.getShort() & 0xFFFF);

        boolean trendPresent = (m.flags & 0x01) != 0;
        boolean qualityPresent = (m.flags & 0x02) != 0;

        boolean warnOctet    = (m.flags & 0x20) != 0;
        boolean calTempOctet = (m.flags & 0x40) != 0;
        boolean statusOctet  = (m.flags & 0x80) != 0;

        int statusLen = (warnOctet ? 1 : 0) + (calTempOctet ? 1 : 0) + (statusOctet ? 1 : 0);
        if (statusLen > 0 && buf.remaining() >= statusLen) {
            byte[] ss = new byte[statusLen];
            buf.get(ss);
            m.sensorStatusAnnunciation = ss;
        }

        if (trendPresent && buf.remaining() >= 2) m.trend = readSfloat(buf);
        if (qualityPresent && buf.remaining() >= 2) m.quality = readSfloat(buf);

        return m;
    }

    private static Float readSfloat(ByteBuffer buf) {
        if (buf.remaining() < 2) return null;
        int raw = buf.getShort() & 0xFFFF;
        int mantissa = raw & 0x0FFF;
        int exponent = (raw >> 12) & 0x000F;
        if ((mantissa & 0x0800) != 0) mantissa = mantissa | 0xFFFFF000;
        if ((exponent & 0x0008) != 0) exponent = exponent | 0xFFFFFFF0;
        double value = mantissa * Math.pow(10, exponent);
        return (float) value;
    }
}
