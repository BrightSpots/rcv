# Universal Tabulator User Guide

## Overview

The Universal Tabulator is a free, open-source application designed to quickly and accurately tabulate ranked choice voting elections using a variety of rules commonly in use in North America.

The Tabulator requires as input a "contest configuration" file which specifies:
- Tabulation rule variations to use
- List of registered candidates 
- One or more cast vote record files
- Output formatting options (contest name, where to write output files, etc.)

The Tabulator produces as output:
- A results spreadsheet including round-by-round vote totals for each candidate and the eventual winner(s)
- An audit report describing how every ballot was counted in each round over the course of the tabulation

The Tabulator provides an interface for:
- Creating contest configuration files
- Validating contest configuration files to ensure they are well-formed, and all values are within expected ranges
- Tabulating a contest with a contest configuration file 

## Installing and Launching the Tabulator

#### Method 1 (Easy): Pre-Compiled Version

1. Download the pre-compiled tabulator for your OS from [here](https://drive.google.com/drive/u/1/folders/1vYYJa5-oJe0lpaVnI_ELn9foZIVZ54pL).

    **Note**: this download should be a "jlink image" which doesn't even require you to have Java installed on your machine.

2. Unzip the file, navigate to the `bin` directory, and launch the GUI by running the `rcv` script if using MacOS or Linux, or `rcv.bat` if using Windows.

#### Method 2 (Less Easy): Compile and Run Using Gradle

1. Install [JDK 11 or higher](https://jdk.java.net/), and make sure your Java path is picking it up properly by verifying that following command returns the expected version:
    
    `$ java -version`
    
    If the expected version isn't getting returned, you'll need to follow the instructions [here](https://www.java.com/en/download/help/path.xml) on how to set your Java path.

2. Download the [zip from GitHub](https://github.com/BrightSpots/rcv/archive/master.zip) and unzip it, or install git and use the following command at the terminal / command prompt to clone a local copy on your machine:
    
    `$ git clone https://github.com/BrightSpots/rcv.git`

3. Use the provided version of Gradle to build and run the code from the terminal / command prompt to start the RCV Tabulator GUI:
    
    `$ cd rcv-master` (or, if you cloned the repo using git: `cd rcv`)
    
    `$ ./gradlew run` (or, if you're on Windows: `gradlew run`)

    If you get a "Permission denied" error in Linux or MacOS, you need to mark the script as executable with:
    
    `$ chmod 777 gradlew`

## Loading and Tabulating a Contest

The Tabulator includes several example contest configuration files and associated cast vote record files.

1. Click "Load..." at the top of the window and browse into the "test_data" folder under src/test/resources.
2. Browse into one of the folders listed here and select the corresponding file that ends with "_config.json".
3. Click on the configuration tabs (Output, CVR Files, Candidates, Rules) to see how this contest is configured.
4. Click "Validate" to check if this configuration is valid. You will see the results in the console area at the bottom of the main window.
5. Click "Tabulate" to tabulate the election. You will see the results in the console, including the location and names of output files.

## Command-Line Interface
Alternatively, you can run the Tabulator using the command-line interface by including the flag `-cli` and then supplying a path to an existing config file, e.g.:

`$ rcv -cli path/to/config`

Or, if you're compiling and running using Gradle:

`$ gradlew run --args="-cli path/to/config"`

Finally, you can activate a special `convert-to-cdf` function via the command line to export the CVR as a CDF JSON instead of tabulating the results, e.g.:

`$ rcv -cli path/to/config convert-to-cdf`

Or, again, if you're compiling and running using Gradle:

`$ gradlew run --args="-cli path/to/config  convert-to-cdf"`

## Viewing Tabulator Output

Tabulator output file names automatically include the current date and time, e.g. `2019-06-25_17-19-28_summary.csv`. This keeps them separate if you tabulate the same contest multiple times.

Look in the console window to see where the output spreadsheet was written, e.g.

`2018-09-09 18:55:53 PDT INFO: Generating summary spreadsheet: /Users/Me/Documents/rcv/test_data/2018_maine_gov_primary_dem/output/2019-06-25_17-19-28_summary.csv`

Double-click on the summary spreadsheet to open it (you may need to install [OpenOffice](https://www.openoffice.org/download/) or some other CSV file reader first).

Find the audit log file and double-click on it to open it. It is a standard text file and can be displayed with any kind of text editor.
