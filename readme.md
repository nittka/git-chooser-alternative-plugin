I used the existing git-chooser-alternative plugin as a basis for adding another build chooser variant.

Our final goal is to allow for a parametrized build, where you can choose a specific branch to build via a git branch parameter.
This parameter variable can be used in branches to build.
In the default-case (`**` build anything or another pattern with wildcards) usually all matching branches would be built, but with the "most recent only" extension only one branch should is built, the one with the latest commit.

One potential problem to investigate is the combination of commit hooks, ignore polling messages and included/excluded paths.
