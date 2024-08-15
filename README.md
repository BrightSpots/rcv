# RCTab

## Overview

RCTab is a free, open-source application designed to quickly and accurately tabulate a wide variety of ranked choice voting (RCV) elections, including both single-winner contests and various multi-winner formats (e.g. single transferable vote, a.k.a. STV). It allows users to:
- Create contest configuration files using a graphical user interface (GUI)
- Validate contest configuration files to ensure they are well-formed, and all values are within expected ranges
- Tabulate a contest

A contest configuration file specifies:
- Which tabulation rule variations to use
- A list of registered candidates 
- Paths to one or more cast vote record (CVR) files
- Output formatting options (contest name, date, jurisdiction, etc.)

The Tabulator produces the following as output:
- A summary .csv file containing round-by-round vote totals for each candidate and the eventual winner(s)
- A summary .json file containing additional information which can be used by external tools for visualizing contest results
- A detailed audit .log file describing how every ballot was counted in each round over the course of the tabulation

## Installing and Launching the Tabulator

#### Method 1 (Easy): Pre-Compiled Version

1. Download the pre-compiled Tabulator for your OS from the GitHub [releases page](https://github.com/BrightSpots/rcv/releases).

    **Note**: this download is a "jlink package", which means you don't even need to have Java installed on your machine to run it!

2. Unzip the file, navigate to the `bin` directory, and launch the RCV Tabulator GUI by running the `rcv` script if using MacOS or Linux, or `rcv.bat` if using Windows.

On Linux, you may install the .deb file, then run `/opt/rcv/bin/RCTab` to launch the tabulator GUI.

#### Method 2 (Less Easy): Compile and Run Using Gradle

1. Install [JDK 21](https://adoptium.net/temurin/releases/?version=21), and make sure your Java path is picking it up properly by
   verifying that the following command returns the expected version:
    
    `$ java -version`
    
    If the expected version isn't returned, you'll need to follow the instructions [here](https://www.java.com/en/download/help/path.xml) on how to set your Java path.

2. Download the [zip of the source code from GitHub](https://github.com/BrightSpots/rcv/archive/master.zip) and unzip it, or install git and use the following command at the terminal / command prompt to clone a local copy on your machine:
    
    `$ git clone https://github.com/BrightSpots/rcv.git`

3. Use the provided version of Gradle to build and run the code from the terminal / command prompt to start the RCV Tabulator GUI:
    
    `$ cd rcv-master` (or, if you cloned the repo using git: `cd rcv`)
    
    `$ ./gradlew run` (or, if you're on Windows: `gradlew run`)

    If you get a "permission denied" error in Linux or MacOS, you need to mark the script as executable with:
    
    `$ chmod 777 gradlew`

#### Method 3 (Least Easy): Building on an Air-Gapped Machine

1. Download Gradle from https://gradle.org/releases/ and place it in your path.
2. Download and extract the source code from
   the [releases page](https://github.com/BrightSpots/rcv/releases).
3. Alongside the release you just downloaded, you will find corresponding cache files (cache.[OS].zip). Download this file too.
4. Stop the Gradle daemon with `gradle --stop`.
5. Delete the directory ~/.gradle/caches if it exists.
6. Extract the appropriate caches/[filename].zip to ~/.gradle/caches so that the "caches" directory is in ~/.gradle.
7. Alongside these extracted caches is a file named checksums.csv. In the extracted directory, you may manually verify each dependency using checksums.csv in accordance with your own policies.
8. Run `gradle assemble --offline` and ensure you get no errors.
9. Run `gradle run --offline` to launch RCTab, or `gradle jpackage --offline` to generate an executable file specific to the OS you are using (a .dmg, .exe, or .deb).

#### Encrypting the Tabulator Directory
For security purposes, we **strongly recommend** applying password encryption (e.g. 256-bit SHA) to the directory containing the Tabulator, config files, CVR files, and any other related files.

We recommend using open-source utilities such as [7-Zip](https://www.7-zip.org/) for Windows or EncFS, gocryptfs, etc. for Linux (see [this comparison](https://nuetzlich.net/gocryptfs/comparison/)). 

Mac OS has built-in encryption capability that allows users to create encrypted disk images from folders using Disk Utility (see ["Create a secure disk image"](https://support.apple.com/guide/disk-utility/create-a-disk-image-dskutl11888/mac)). 

## Configuring a Contest

The GUI can be used to easily create, save, and load contest configuration files (which are in .json format). These files can also be created manually using any basic text editor, but this method isn't recommended.

In either case, please reference the [config file documentation](config_file_documentation.txt) when configuring a contest.

**Warning**: Using shortcuts, aliases, or symbolic links to launch the Tabulator is not supported; doing so may result in unexpected behavior. Also, please avoid clicking in the command prompt / terminal window when starting the Tabulator GUI, as it may halt the startup process.

## Loading and Tabulating a Contest

The Tabulator includes several example contest configuration files and associated CVR files.

1. Click "File > Load..." in the menu and navigate to the `sample_input` folder (if you used Method 2 to install the Tabulator, navigate to the `test_data` folder).
2. Open one of the folders listed here and select the config file (it will have the `_config.json` suffix).
3. Click on the configuration tabs (Output, CVR Files, Candidates, Required Rules, Optional Rules) to see how this contest is configured.
4. Click "Tabulation > Validate" in the menu to check if this configuration is valid. You will see the results in the console at the bottom of the main window.
5. Click "Tabulation > Tabulate" in the menu to tabulate the election. You will see the results in the console, including the location of the output files.

## Command-Line Interface

Alternatively, you can run the Tabulator using the command-line interface by including the flag `--cli` and then supplying a path to an existing config file, e.g.:

`$ rcv --cli path/to/config`

Or, if you're compiling and running using Gradle:

`$ gradlew run --args="--cli path/to/config"`

You can also activate a special `convert-to-cdf` function via the command line to export the CVR as a NIST common data
format (CDF) .json instead of tabulating the results, e.g.:

`$ rcv --cli path/to/config --convert-to-cdf`

This option is available in the GUI by selecting the "Conversion > Convert CVRs in Current Config to CDF" menu option.

Or, again, if you're compiling and running using Gradle:

`$ gradlew run --args="--cli path/to/config --convert-to-cdf"`

Note: if you convert a source to CDF and that source uses an overvoteLabel or an undeclaredWriteInLabel, the label will
be represented differently in the generated CDF source file than it was in the original CVR source. When you create a
new config using this generated CDF source file and you need to set overvoteLabel, you should use "overvote". If you
need to set undeclaredWriteInLabel, you should use "Undeclared Write-ins".

## Viewing Tabulator Output

Tabulator output filenames automatically include the current date and time,
e.g. `2019-06-25_17-19-28_summary.csv`. This keeps them separate if you tabulate the same contest
multiple times.

Look in the console window to see where the output spreadsheet was written, e.g.

`2019-06-25 17:19:28 PDT INFO: Generating summary spreadsheet: /rcv/test_data/2018_maine_gov_primary_dem/output/2019-06-25_17-19-28_summary.csv...`

The summary spreadsheet (in .csv format), summary .json, and audit .log files are all readable using a basic text editor.

**Note**: If you intend to print any of the output files, we **strongly recommend** adding headers /
footers with page numbers, the filename, the date and time of printing, who is doing the printing,
and any other desired information.

## Acknowledgements

#### Bright Spots Developers

- Jonathan Moldover
- Louis Eisenberg
- Hylton Edingfield
