I used the existing git-chooser-alternative plugin as a basis for another build chooser variant.

Our final goal is to allow for a parametrized build, where you can choose a specific Branch to build.
In the default-case (`**` build anything or another pattern with wildcards) only one branch should be built, the one with the latest commit.

The problem is that with multiple branches to build, all are built - even if their last commit was ignored due to a ignore path or ignore poll commit message. The additional behaviour "build single revision only" is not enough because it seems not to sort the branches in the reverse commit order.