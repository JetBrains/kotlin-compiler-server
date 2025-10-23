# Workflow – kotlin-compiler-server backend team
This document describes the branching strategy and release flow for our kotlin-compiler-server repository.

## Branching Strategy

* **master**

  * Always reflects the latest stable state.

* **\<version\>**
  * This branch contains the compiler server with the Kotlin version corresponding to the branch name.

* **kotlin-community/dev**
  * This branch is the development branch. All the new features should be merged into this branch.

* **kotlin-community/\<version\>**
    * This branch is the prelease development branch for the exact Kotlin version.
* **feature/KTL-54321-***

  * Feature branches, created from the current `kotlin-community/dev` branch.

  * Naming: `feature/KTL-<task-id>-<short-description>`

* **hotfix/\<version\>/KTL-54321-***

  * Hotfix branches are created from the target version branch.

  * Naming: `hotfix/<version>/KTL-<task-id>-<short-description>`

## Feature development flow
This block describes working on a new feature flow.
1. Create a branch from the latest `kotlin-community/dev`:
2. Implement your changes in this branch.
3. Cover your changes with tests.
4. Move task to `Code Review` state in YouTrack.
5. Open a Pull Request to a `kotlin-community/dev` branch. 
6. After review approval: merge Pull Request and move a task to `Ready for Deploy` state in YouTrack.

## Release Flow
This block describes the process of releasing a new version.
[More about Kotlin release types.](https://kotlinlang.org/docs/releases.html)
1. Create a branch with the version name:
   * if it is a `bug fix` or `tooling` release create branch from the previous release branch
   * if it is `language` release version (`Beta*` and `RC*` are included):
     * if specific `kotlin-community/<stable-version>` branch exists, create branch from it
     * otherwise:
       * for `Beta*` version create new branch from `kotlin-community/dev` branch
       * for `RC*` and stable versions create a branch from the previous release branch within that version line.
2. Modify [the Kotlin version](https://github.com/JetBrains/kotlin-compiler-server/blob/master/gradle/libs.versions.toml#L2) in the branch.
3. If the current Kotlin version is bigger than in `master`, then create a pull request to `master`. Otherwise, push your changes to the remote branch and skip the rest of the steps.
   * creating a pull request should be from the separate branch like `merge/2.0.0-RC2`
   * use rebase instead of merge for resolving conflicts
   * after the pull request is merged, delete the `merge/*` branch.
4. Wait until the tests are completed successfully.
5. Merge the branch to master.

## Hotfix Flow
1. Create a branch from `<version>`:
    ```bash
      git checkout -b hotfix/KTL-<task-id>-<short-description> origin/<version>
    ```
2. Implement your fix in this branch.
3. Move task to `Code Review` state in YouTrack.
4. Open a Merge request to the `<version>` branch.
5. After review approval: move the task to `Ready for Deploy` state in YouTrack.
6. After all tests are passed, merge the branch to `<version>`.
7. Wait until the [deployment on TeamCity](https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_KotlinSites_Deployments_PlayKotlinlangOrg_Backend_DeployWithPulumi?branch=%3Cdefault%3E&buildTypeTab=overview#all-projects) is completed (it will appear not immediately)
8. Move task to `Fixed` state in YouTrack.