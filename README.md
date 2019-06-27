# Ranked Choice Voting Universal Tabulator

## Dependencies

#### Java
[JDK 11+](https://jdk.java.net/) (OpenJDK recommended)

#### Other Dependencies
See the `dependencies` entry in the [Gradle build file](build.gradle). Gradle should be used to automatically download these libraries.

## Overview

The Universal Tabulator is a free, open-source application designed to quickly and accurately tabulate a wide variety of ranked choice voting elections. It allows users to:
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

1. Download the pre-compiled tabulator for your OS from the Ranked Choice Voting Resource Center's Google Drive [here](https://drive.google.com/drive/u/1/folders/1vYYJa5-oJe0lpaVnI_ELn9foZIVZ54pL).

    **Note**: this download should be a "jlink image" which doesn't even require you to have Java installed on your machine.

2. Unzip the file, navigate to the `bin` directory, and launch the RCV Tabulator GUI by running the `rcv` script if using MacOS or Linux, or `rcv.bat` if using Windows.

#### Method 2 (Less Easy): Compile and Run Using Gradle

1. Install [JDK 11 or higher](https://jdk.java.net/), and make sure your Java path is picking it up properly by verifying that the following command returns the expected version:
    
    `$ java -version`
    
    If the expected version isn't returned, you'll need to follow the instructions [here](https://www.java.com/en/download/help/path.xml) on how to set your Java path.

2. Download the [zip of the source code from GitHub](https://github.com/BrightSpots/rcv/archive/master.zip) and unzip it, or install git and use the following command at the terminal / command prompt to clone a local copy on your machine:
    
    `$ git clone https://github.com/BrightSpots/rcv.git`

3. Use the provided version of Gradle to build and run the code from the terminal / command prompt to start the RCV Tabulator GUI:
    
    `$ cd rcv-master` (or, if you cloned the repo using git: `cd rcv`)
    
    `$ ./gradlew run` (or, if you're on Windows: `gradlew run`)

    If you get a "Permission denied" error in Linux or MacOS, you need to mark the script as executable with:
    
    `$ chmod 777 gradlew`

## Configuring a Contest

The GUI can be used to easily create, save, and load contest configuration files (which are in .json format). These files can lso be created manually using any basic text editor, but this method isn't recommended.

In either case, please reference the [config file documentation](src/main/resources/network/brightspots/rcv/config_file_documentation.txt) when configuring a contest.

## Loading and Tabulating a Contest

The Tabulator includes several example contest configuration files and associated CVR files.

1. Click "Load..." at the top of the window and navigate to the "test_data" folder.
2. Open one of the folders listed here and select the config file (it will have the "_config.json" suffix).
3. Click on the configuration tabs (Output, CVR Files, Candidates, Rules) to see how this contest is configured.
4. Click "Validate" to check if this configuration is valid. You will see the results in the console at the bottom of the main window.
5. Click "Tabulate" to tabulate the election. You will see the results in the console, including the location of the output files.

## Command-Line Interface

Alternatively, you can run the Tabulator using the command-line interface by including the flag `-cli` and then supplying a path to an existing config file, e.g.:

`$ rcv -cli path/to/config`

Or, if you're compiling and running using Gradle:

`$ gradlew run --args="-cli path/to/config"`

Finally, you can activate a special `convert-to-cdf` function via the command line to export the CVR as a NIST common data format (CDF) .json instead of tabulating the results, e.g.:

`$ rcv -cli path/to/config convert-to-cdf`

Or, again, if you're compiling and running using Gradle:

`$ gradlew run --args="-cli path/to/config  convert-to-cdf"`

## Viewing Tabulator Output

Tabulator output file names automatically include the current date and time, e.g. `2019-06-25_17-19-28_summary.csv`. This keeps them separate if you tabulate the same contest multiple times.

Look in the console window to see where the output spreadsheet was written, e.g.

`2019-06-25 17:19:28 PDT INFO: Generating summary spreadsheet: /rcv/test_data/2018_maine_gov_primary_dem/output/2019-06-25_17-19-28_summary.csv...`

The summary spreadsheet (in .csv format), summary .json, and audit .log files are all readable using a basic text editor.

## Acknowledgements

#### Bright Spots Developers

- Jonathan Moldover
- Louis Eisenberg
- Hylton Edingfield
