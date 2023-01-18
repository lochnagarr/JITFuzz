## Getting Started

### Directory description

```
benchmark:  projects as seeds
logs:		log files
out:		builded class files
resources:	configuration files of seed projects
seeds:		seed files generated
src:		source code
tmp:		temporary files during experiment
util:		helper class(utils.Digit)
```

### How to Run
#### Prerequisites
1. JDK11

2. AFLplusplus
   - Get by `git clone https://github.com/AFLplusplus/AFLplusplus.git`

3. JDK source code
   - Get by `git clone https://github.com/openjdk/jdk.git`
#### Steps to build
1. Build AFLplusplus
   - See `https://github.com/AFLplusplus/AFLplusplus/blob/stable/docs/INSTALL.md`
2. Build JDK with AFLplusplus
    - Run 
    ```
    CC=PATH/TO/AFLPLUSPLUS/afl-gcc-fast CXX=PATH/TO/AFLPLUSPLUS/afl-g++-fast bash configure --enable-debug --disable-warnings-as-errors
    AFL_GCC_ALLOWLIST=PATH/TO/AFL_GCC_ALLOWLIST make images
    # Please use the full path of AFL_GCC_ALLOWLIST.
    ```
3. Build JITFuzz
   - You can import it into IntelliJ IDEA workspace
   - Build and run `JITFuzz` with `JDK11`
#### Steps to run
1. Choose and build a seed project
2. Write a `seed_project.properties` file and put it in `./resources`; See `./resources/fastjson.properties` as example.
3. run `experiment.Main` with `JDK11`.

   Command line options:
   - `-i: The index of this experiment, used to mark the log and seeds files. e.g., 0`
   - `-I: The total iterations of mutation. e.g., 1000`
   - `-p: The project that serves seed files ./resources/project.properties will be read. e.g., fastjson`
   - `-s: The path to store seeds generated. Not necessary, default value: ./seeds`
   - `-l: The path to store log files. Not necessary, default value: ./logs`
   - `-e: The mutator to be disabled: scalar|escape|simp|inline|wrap|trans; Not necessary,
            no one will be disabled in default.`

