# Branching Guide

This repository now includes a dedicated feature branch for the AiDEXX CGM BLE workflow so
that the experimental implementation can evolve without affecting `work` until the changes are
validated on real hardware.

## Current Branches
- `work`: main development branch with previously validated functionality.
- `aidexx-cgm-workflow`: feature branch created for continued AiDEXX CGM work.

## Pushing the New Branch
To publish the new branch to your remote, run the following from a local environment that has
push access to the repository:

```bash
git checkout aidexx-cgm-workflow
git push -u origin aidexx-cgm-workflow
```

After the initial push, subsequent updates can be pushed with:

```bash
git push
```

Remember to keep the main branch up to date locally before merging:

```bash
git checkout work
git pull origin work
git checkout aidexx-cgm-workflow
git rebase work
```
