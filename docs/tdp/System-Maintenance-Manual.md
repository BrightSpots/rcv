# RCTab v1.3.2 Section 08 - System Operations Procedures v.1.2.2

> **RCTab v1.3.2 Section 08 - System Operations Procedures v.1.2.2**
> document is solely for use in the State of California. This document
> can be expanded or updated as is necessary or required. Where
> relevant, this document refers to specific sections and requirements
> of the California Voting System Standards. Any recommendations listed
> in this document should not supersede user jurisdiction procedures or
> other controlling governance entities.

Users should ensure have been followed and completed prior to operating
RCTab:

> **<span class="mark">RCTab v1.3.2 Section 05 - Acceptance Test
> Procedures v.1.2.1</span>**
>
> **<span class="mark">RCTab v1.3.2 Section 16 - System Hardening
> Procedures - Windows OS v.1.3.0</span>**
>
> **<span class="mark">RCTab v1.3.2 Section 22 - Installation
> Instructions for Windows OS v.1.3.0</span>**
>
> **<span class="mark">RCTab v1.3.2 Section 23 - HashCode Instructions -
> Windows OS v.1.2.1</span>**

**<span class="mark">RCTab v1.3.2 Section 25 - Configuration File
Parameters v.1.1.1</span>**

Any interaction with RCTab, including producing configuration files,
running tabulations, hashing results files, and transmission of files
from RCTab on USB drives should follow transmission procedures required
in jurisdiction, including the use of a team with no less than two
trained personnel.

## Launching RCTab

The manufacturer recommends RCTab be installed as part of the
pre-election preparation process. In order to determine the appropriate
launch procedure for a jurisdiction, users should consider the maximum
possible number of votes that could occur in the event all eligible
voters presented themselves to vote. Jurisdictions should, as part of
their pre-election procedure, launch RCTab according to the relevant
launch instructions described below to ensure they launch RCTab in
accordance with the below requirements. The manufacturer is available
for support with this process. For acceptance testing and L&A procedures
to set up use of RCTab, see **<span class="mark">RCTab v1.3.2 Section
11 - L&A Testing v.1.3.2</span>** and **<span class="mark">RCTab v1.3.2
Section 05 - Acceptance Test Procedures v.1.2.1.</span>**

Contests with fewer than 1,000,000 votes

To Launch:

1.  Navigate to the rcv folder created when you unzipped the Tabulator.

2.  Open the bin folder

3.  Right- click on the rcv.bat file. Click ‘Run as Administrator.’ If a
    ‘Windows protected your PC’ window pops up click ‘More Info’ then
    click the ‘Run anyway’ button. Enter the administrator password

Contest with more than 1,000,000 votes

1.  Open a Command Prompt by navigating to the start menu and typing in
    Command Prompt.

2.  Press enter to launch Command Prompt.

3.  Change the current directory to the rcv folder created when you
    unzipped the Tabulator.

4.  First, type in cd (note there should be a space after cd).

5.  Using File Explorer navigate to the folder where RCTab is installed.

6.  Double click on the rctab\_v1.3.0\_windows folder

7.  Click and drag the rcv folder over to the command prompt window

8.  Your command prompt will now read something like:

    1.  cd C:\RCTab\rctab\_v1.3.0\_windows\rcv

9.  Press enter

10. Launch the tabulator by entering the following command:

    1.  .\bin\java -mx30G -p .\app -m
        network.brightspots.rcv/network.brightspots.rcv.Main .

## Prepping CVRs for use with RCTab Software

All export of CVR records for use with RCTab and migration of CVR
records for use with RCTab should follow CVR procedures required by the
jurisdiction. Users must keep track of the path to each file that is
needed to tabulate the RCV contests. You will need it when you configure
the RCTab software.

When tabulation begins, RCTab automatically, programmatically verifies
the cryptographic signature of all Hart CVRs used as input for contest
tabulation. This verification step ensures both the integrity (CVR
contents have not been edited) and provenance (CVRs came from the Hart
voting system) of the Hart CVRs.

Users should confirm that each Hart CVR .xml file used as input for
RCTab has a corresponding .sig.xml file. Instructions to enable signed
Hart CVRs are available below in the “CVR Files Options - Hart” heading
below.

## Setting up a configuration file for RCTab

All settings in the RCTab software are described in technical detail in
the **<span class="mark">RCTab v1.3.2 Section 25 - Configuration File
Parameters v.1.1.1</span>**, including brief descriptions of how options
impact the operational validity of other options. This document will
describe how to set up a configuration for a contest using ranked choice
voting rules. This guide also includes screenshots of the interfaces
described. The values a user inputs into any of these fields depend upon
the relevant laws and regulations in place in their jurisdiction, as
well as the voting system vendor used to produce cast-vote records for
their elections. Users must understand the requirements of their laws,
regulations, and vendor CVR data in order to fill out these fields
accurately for their needs.

RCTab is organized into tabs: the Contest Info Tab, the CVR Files Tab,
the Candidates Tab, the Winning Rules Tab, the Voter Error Rules Tab,
and the Output Tab. These tabs each include a set of fields that users
fill out - some fields are always required, some fields are required
based on previous input, and some fields are always optional. This guide
will take the user through basic descriptions of each of these tabs and
each of these fields. Additional technical information about these
fields can be found in the **<span class="mark">RCTab v1.3.2 Section
25 - Configuration File Parameters v.1.1.1</span>**. As users navigate
through each tab they are building a configuration .json file which must
be saved when complete. If RCTab is closed and the configuration is not
saved, no information will be stored. The next time RCTab is opened all
fields will be blank. Once a configuration is saved, it can be used by
RCTab to process RCV election results according to the rules laid out in
that configuration, provided all rules necessary are filled out.

Each individual contest run through RCTab requires its own configuration
with contest-specific information such as contest name and candidate
names. This guide will run through setting up the winning rules and
voter error rules requirements that will apply to your jurisdiction, and
will discuss procedures for how to properly fill out other fields in the
software. Operation of RCTab must be conducted by a team of no less than
2 trained personnel.

The following guide describes how to operate RCTab. See also
**<span class="mark">RCTab v1.3.2 Section 11 - L&A Testing
v.1.3.2</span>** document for additional detail on system operations.

### Contest Info Tab

Below is what the contest info tab looks like when a user first
navigates to it. This is also the first screen a user will see upon
successfully launching RCTab. Contest info fields are in the middle of
the screen. The black box at the bottom of the screen is called the
**operation log box**. It sends the user messages about the tabulation
process, any errors encountered in using a configuration file, any
errors in tabulation, and other software errors. Information in this
panel is saved to the rcv\_0.log file. Users cannot turn this feature
off using the RCTab software. The panel on the right side of the
document is the “Hints'' tab which includes information for how to fill
out each screen successfully.

<img src="../media/image17.png"
style="width:6.44792in;height:1.9838in" />

These settings appear on the “Contest Info” tab inRCTab.

-   Contest Name

-   Contest Date

-   Contest Jurisdiction

-   Contest Office

-   Rules Description

The tabulator calculates results for one contest at a time, rather than
the results of several contests for an election all at once. The
information on this tab is for the particular contest you will be
tabulating.

These fields do not influence the computations. Contest Name, Contest
Date, Contest Jurisdiction, and Contest Office are shown in the final
output file(s) to help connect the data (results) with the contest the
results belong to.

-   Think long term, e.g. from the perspective of looking at the results
    files 6 months after the election and wanting to be clear what
    contest the results belong to.

-   You may find it helpful to revisit this tab once you have done a few
    test runs and see what the output looks like.

Contest Name (required): Enter a name to identify it.

Examples: City Council 2018, Board of Election Ward 13 2017, Mayor,
Referendum 289b

Contest Date (optional): The date on which the election for this contest
was run.

-   Clear permits user to clear this field

-   The calendar button allows a user to navigate through a calendar to
    select the date of the contest

Contest Jurisdiction (optional): E.g.: Minneapolis, Eastpointe

Whether this is helpful may depend on what you put into the Contest Name
field

Contest Office (optional): E.g.: Mayor, County Clerk

Whether this is helpful may depend on what you put into the Contest Name
field

Rules Description (optional): What short description of this
configuration would help you remember in, say, six months what election
this specific rule configuration is for? This option’s use impacts no
other options. It is included in configuration .json files and audit
.log files.

<img src="../media/image19.png"
style="width:6.48958in;height:2.11125in" />

### CVR Files Options

This is what the CVR Files tab looks like when a user first navigates to
it.

<img src="../media/image21.png"
style="width:6.5in;height:4.30061in" />

The tabulator needs a configuration file laying out where each of its
user’s CVR files for the contest to be tabulated are and how to
interpret each of them. Only add files to the configuration that contain
data for the contest you are tabulating. CVRs that do not include data
for the contest to be tabulated will cause tabulation to fail. Fields
used to describe CVRs are on the top half of the tab, while the table on
the bottom half of the tab will display each CVR file you have added to
your configuration file so far. All information in this tab directs
RCTab on where to find files and how to read those files, it does not
actually pull in any vote information. Vote information is only used
after a user clicks the “Tabulate'' button under “Tabulation” as
described later in this guide. This guide will now briefly discuss how
to set up RCTab to read a Hart CVR.

#### Hart

<img src="../media/image1.png"
style="width:6.45833in;height:4.26571in" />Permits user to edit these
options:

-   Path

-   Contest ID

-   Undeclared Write-In Label

Does not permit the user to edit this option:

-   First Vote Column

-   First Vote Row

-   ID Column

-   Precinct Column

-   Overvote Delimiter

-   Overvote Label

-   Undervote Label

-   Treat Blanks as Undeclared Write-in

Path: Select the path of the folder that contains signed Hart CVRs. That
folder should contain CVR files in .xml format. Each .xml should also
have a corresponding \[fileName\].sig.xml. The \[fileName\].sig.xml
files are used to verify the cryptographic signature of each .xml CVR.
Tabuluation will not proceed unless the signature can be verified. Use
the Preferences menu in Verity Count to digitally sign CVR exports,
described below.

<img src="../media/image14.png"
style="width:6.5in;height:8.86111in" />

Contest ID (required for Hart): Contest ID must be completed before
continuing onto the next step. The contest ID is provided by Hart’s
Verity System and is specific to the Ranked Choice Voting contest.

Undeclared Write-in Label (optional): Some CVRs use a particular
word/phrase to indicate a write-in. Set to the label used for undeclared
write-ins in your CVR

Add: Adds CVR to configuration file. Impacts no other option. Operation
impacted by whether required fields (depending on Provider selected) are
filled in.

Clear: Clears all fillable values in CVR Files tab above the CVR Files
table.

Delete Selected: Deletes CVR file information listed in the CVR table.

####  

#### Candidates Tab

The candidates tab allows users to put in information about how
candidates are referred to in cast-vote record files. These settings
impact how CVR files are read. Candidate names entered on this tab will
also be used to display candidate names in results files. Below is a
screenshot of the Candidates Tab when a user first navigates to it.
Candidate Names must match names as represented in CVR files. Users
should record all candidate names as recorded in the CVR file in order
to successfully input them on this tab. Users should export a candidate
listing from the election management system and refer to it when filling
out this tab.

<img src="../media/image5.png"
style="width:6.52264in;height:5.46751in" />

Fill in the fields and click the Add button:

Name (required): E.g.: Dave Harris. This information is used to display
candidate names in results files. Fill in with correct names for
candidates in this contest.

Code (required for Hart): Hart CVR files use codes in lieu of full
candidate name. E.g.: "JCD" or "14" and must be included when adding
candidates to RCTab. Candidate codes are provided by Hart’s Verity
System.

Excluded (optional): When checked, the candidate will be ignored during
tabulation. An example of when this might be used: a candidate dropped
out after the ballots were printed.

Add: Adds Name, code, and/or excluded information to the Candidates
Table.

Clear: Clears any information in Name, Code, and Excluded.

Delete Selected: Deletes selected candidate data from the Candidate
Table.

### Winning Rules Options

Winning Rules options tell RCTab what kind of ranked choice voting
election to run and how to handle details required for each kind of
ranked choice voting RCTab can run.

This is a screenshot of what winning rules will look like when a user
first navigates to it.

<img src="../media/image16.png"
style="width:6.54167in;height:2.90632in" />

There are two main sets of options on the Winning Rules tab: Winner
Election Mode options and Tiebreak Mode options.

Winner Election Mode (required): Before any option is available, users
must select the type of RCV election mode they are tabulating. The
options currently available are:

-   Single-winner majority determines winner

-   Multi-winner allow only one winner per round

-   Multi-winner allow multiple winners per round

-   Bottoms-up

-   Bottoms-up using percentage threshold

-   Multi-pass IRV

More information about these options can be found in
**<span class="mark">RCTab v1.3.2 Section 18 - User Guide
v.1.2.2</span>**, under the section **Winning Rules Options.** Options
in this section will be available based on which Winner Election mode is
selected.

Maximum Number of Candidates That Can Be Ranked: Users have the option
to select the maximum number of candidates available (checked by
default), or unselect the checkbox and enter the correct number of
rankings allowed by their jurisdiction in the text box.

Minimum Vote Threshold: This setting may be left blank or set to 0
unless jurisdiction rules specify otherwise.

Use Batch Elimination: This setting is unchecked by default but if
checked allows for use of Batch Elimination in tabulation.

Continue until Two Candidates Remain: Selecting this option will run the
round-by-round count until only two candidates remain, regardless of
when a candidate wins a majority of votes.

Number of Winners: The number of seats to be filled in the contest.
RCTab automatically sets this value to 1 when “Single-winner majority
determines winner.”

Percentage Threshold: The share of votes a candidate must have in order
to win by Percentage.

Threshold Calculation Method: Preloaded calculation methods based on the
most common multi-winner RCV threshold formulas.

Decimal Places for Vote Arithmetic: Sets how many decimal places after
the decimal point are used in surplus transfers and in calculating the
threshold.

#### Tiebreaking

Tiebreak Mode: Users are given the option to select one of six tiebreak
methods. They are:

-   Random

-   Stop Counting and ask

-   Previous round counts (then random)

-   Previous round counts (then stop counting and ask)

-   Use candidate order in the config file

-   Generate permutation

Random Seed: Required if Tiebreak Mode is "Random", "Previous round
counts (then random)", or "Generate permutation.” Users can then enter a
positive or negative integer to generate random orders.

Users should select the “Stop Counting and ask” option. If a tie occurs
between candidates before the final round of counting, the software will
pop up a window asking users to select which candidate will lose the
tie. Users should follow tiebreaking protocols set by their jurisdiction
(such as drawing lots) then input the result of that protocol into the
software via the popup.

If a tie occurs in the final round of counting, Eureka law requires the
winner of the tie be the candidate who led with more first choices in
the first round. There is no specific setting for this tiebreaking
option in the RCTab software. If a tie occurs in the final round, users
should note down the tied candidates then select the “cancel” option in
the popup dialog. Users can then inspect the audit log for that
tabulation (following the instructions under **Retrieving Audit Log**
and **Reviewing Audit Log** in **<span class="mark">RCTab v1.3.2 Section
28 - Post-Election Audit & Clearing RCTab from System v.1.1.1</span>**)
to determine which candidate led in first choices in the first round.
Record that candidate’s name. Re-run the tabulation following the steps
in this guide and select the first-round leader when the tie-breaking
pop up dialog appears.

For more information about any of these options, see
**<span class="mark">RCTab v1.3.2 Section 18 - User Guide
v.1.2.2</span>** and the section **Winning Rules Options**.

####  

### Voter Error Rules

The tabulator needs to know how to handle voter errors in your
jurisdiction. These requirements are typically included in statute or
regulation. This is a screenshot of what winning rules will look like
when a user first navigates to it.

<img src="../media/image35.png"
style="width:6.27083in;height:1.86247in" />

Overvote Rule (required): Users have the option to select how to handle
a ballot where a voter has marked multiple candidates at the same
ranking when that ballot is encountered in the round-by-round count. The
options are:

-   Always skip to next rank

-   Exhaust immediately

-   Exhaust if multiple continuing

How Many Consecutive Skipped Ranks Are Allowed (required): Users are
given the option to select how many rankings in a row can a voter skip
and still have later rankings count. 0 allows no skipped rankings, while
1 allows voters to skip rankings one at a time, but not more than 1 in a
row, and so on.

The Unlimited checkbox allows for an unlimited number of skipped
rankings.

Exhaust on Multiple Ranks for the Same Candidate (optional): When
checked, the tabulator will exhaust a ballot that includes multiple
rankings for the same candidate when that repeat ranking is reached.

####  

### Output Tab

Tell the tabulator where results files go and what additional results
files you want. This is a screenshot of what output looks like when a
user first navigates to it.

<img src="../media/image44.png"
style="width:6.5in;height:2.85997in" />

Output directory: One folder per contest tabulated should be created.
Output directory instructs RCTab where to save any output files from
successful RCTab tabulations. This is the location where results files
will go. If no value (or a relative path, like "output") is supplied,
the location where the config file is saved will be used as the base
directory. Absolute paths, like "C:\output" work too. Select allows
users to navigate through Explorer to select a location.

RCTab will not allow the output path to be configured to a Windows user
account folder (by default anything under the path c:\users\\. This
requirement ensures read-only output. You **must** follow the
instructions in **RCTab v1.3.2 Section 22 - Installation Instructions
for Windows OS v.1.2.2** to set permissions on the output folder.
Following these instructions ensures that the ‘RCTab’ user account
cannot edit or delete RCTab summary output files or audit logs.

Tabulate by Precinct: This checkbox allows RCTab to produce
round-by-round results at the precinct level. However, the CVR file must
contain precinct information for this to function correctly.

Generate a CDF JSON: Produces a VVSG common data format JSON file of the
CVR.

## Generating Results Files 

### Validating Configuration files

Once a configuration file is successfully created, the user must
validate the configuration file.

1.  Click on “Tabulation” at the top of the software window

2.  Click on “Validate”

3.  Refer to the log box at the bottom of the application. If the
    message “Contest config validation successful.” appears, your
    contest configuration has been successfully completed.

4.  If any error messages appear in the log box, refer to
    **<span class="mark">RCTab v1.3.2 Section 29 - RCTab Operator Log
    Messages v.1.2.2</span>** and messages in the log box for how to
    resolve errors. If the error persists, restart RCTab software

### Saving Configuration Files

Once ready to run a tabulation, the user must first save the
configuration file.

1.  Click on “File” at the top of the software window

2.  Click on “Save…”

3.  Select a location to save the configuration file. manufacturer
    suggests users save the configuration file to the same location set
    in the Output Directory setting.

4.  Refer to the log box at the bottom of the application. If the
    message “Successfully saved file: Filepath” your configuration .json
    file has been successfully saved.

5.  If any error messages appear in the log box, refer to
    **<span class="mark">RCTab v1.3.2 Section 29 - RCTab Operator Log
    Messages v.1.2.2</span>** and messages in the log box for how to
    resolve errors. If error persists, restart RCTab software.

### Running a Tabulation

Once a configuration is saved, the user is ready to run a tabulation.

1.  Click on “Tabulation” at the top of the software window

2.  Click on “Tabulate”

3.  Tabulation will begin.

4.  If all above steps were successfully completed, Tabulation will run
    until complete.

5.  Tabulator log box will update with messages as Tabulation proceeds.

6.  Once complete, Tabulator log box will display message stating
    “Results written to: \[filepath from Output Directory\]”

Output files will be:

-   .csv contest summary files

    -   summary.csv Whole-contest summary files

    -   summary.csv.hash Corresponding hash file. This contains the hash
        to verify the results in summary.csv

    -   Precinct-by-precinct summary files (if tabulating by precinct)

-   .json contest summary files

    -   summary.json Whole-contest summary files

    -   summary.json.hash Corresponding hash file. This contains the
        hash to verify the results in summary.json

    -   Precinct-by-precinct summary files (if tabulating by precinct)

-   .log audit files

    -   .log audit files are exported in 50MB sections. If a .log file
        exceeds 50MB an additional .log file is started by RCTab

    -   Corresponding audit\_N.log.hash file. This contains the hash to
        verify the information in each audit\_*N*.log file

-   .json CDF (common data format) files if Generate a CDF JSON is
    checked

More information about results files is available in **RCTab v1.3.2
Section 02 - Software Design and Specifications
v.1.4.2<span class="mark">. </span>**<span class="mark">If
necessary**,** instructions for verifying result file hashes can be
found in **RCTab v1.3.2 Section 23 - HashCode Instructions - Windows OS
v.1.2.1.**</span>

Users can then navigate to “File” and click “Exit” if all contests are
tabulated.

If more contests remain to be tabulated, and contests will contain fewer
than 1,000,000 total votes, users can navigate to “File” and click
“New.” This will clear all fields in RCTab and permit the user to create
a new configuration file.

If contests to be tabulated will contain more than 1,000,000 total
votes, return to start of guide and re-launch tabulator according to
large configuration launch requirements. Then follow the guide to set up
a configuration file.

If any errors arise in the use of RCTab, refer to
**<span class="mark">RCTab v1.3.2 Section 29 - RCTab Operator Log
Messages v.1.2.2</span>**. Errors arising out of any hardware or
software other than RCTab should refer to **<span class="mark">RCTab
v1.3.2 Section 09 - System Maintenance Manual v.1.3.2</span>** and any
relevant user and maintenance manuals.

Before publishing results, jurisdictions should use their established
reconciliation procedures to ensure total votes counted in each round
equals total ballots cast in the contest. If numbers in the
reconciliation process do not match, user should double-check that all
CVRs for that contest were exported successfully from the voting system
and run RCTab process for that contest again. Rely on user jurisdiction
CVR handling procedures for transmitting CVRs.

Any interaction with RCTab, including producing configuration files,
running tabulations, hashing results files, and transmission of files
from RCTab on USB drives should follow transmission procedures required
in the jurisdiction, including the use of a team with no less than two
trained personnel.

Required capabilities that may be bypassed or deactivated during
installation or operation by the user shall be clearly indicated.

Additional capabilities that function only when activated during
installation or operation by the user shall be clearly indicated.

Additional capabilities that normally are active but may be bypassed or
deactivated during installation or operation by the user shall be
clearly indicated.

The installation process for RCTab does not give users the opportunity
to bypass or deactivate options or settings.

Capabilities that are active or inactive in software operation depend on
various factors. Many of these factors are laid out above. Configuration
files determine which system capabilities apply to a given set of voting
data. More information about operations that users can set through the
user interface is provided in **<span class="mark">RCTab v1.3.2 Section
25 - Configuration File Parameters v.1.1.1</span>**. Information about
operation of those settings is also provided in
**<span class="mark">RCTab v1.3.2 Section 02 - Software Design and
Specifications v.1.4.2</span>**. <span class="mark"></span>

## Configuration File Parameters and UI Label Match Sheet

Below is a list of all configuration file parameter labels and their
corresponding labels in the RCTab UI. Labels are organized by the order
of their appearance in the RCTab UI.

**Configuration File Parameters Name/Label | UI Name/Label**

contestName | Contest Name

contestDate | Contest Date

contestJurisdiction | Contest Jurisdiction

contestOffice | Contest Office

rulesDescription | Rules Description

provider | Provider

cdf | CDF

clearBallot | Clear Ballot

Dominion | Dominion

ess | ES&S

hart | Hart

filePath | Path

contestId | Contest ID

firstVoteColumnIndex | First Vote Column

firstVoteRowIndex | First Vote Row

idColumnIndex | ID Column

precinctColumnIndex | Precinct Column

overvoteDelimiter | Overvote Delimiter

overvoteLabel | Overvote Label

undervoteLabel | Undervote Label

undeclaredWriteInLabel | Undeclared Write-in Label

treatBlankAsUndeclaredWriteIn | Treat Blank as Undeclared Write-In

name | Name

code | Code

excluded | Excluded

winnerElectionMode | Winner Election Mode

singleWinnerMajority | Single-Winner Majority Determines Winner

multiWinnerAllowOnlyOneWinnerPerRound | Multi-Winner Allow Only One
Winner Per Round

multiWinnerAllowMultipleWinnersPerRound | Multi-Winner Allow Multiple
Winners Per Round

bottomsUp | Bottoms-up

bottomsUpUsingPercentageThreshold | Bottoms-up using Percentage
Threshold

multiPassIrv | Multi-Pass IRV

maxRankingsAllowed | Maximum Number of Candidates That Can Be Ranked

minimumVoteThreshold | Minimum Vote Threshold

batchElimination | Use Batch Elimination

continueUntilTwoCandidatesRemain | Continue Until Two Candidates Remain

tiebreakMode | Tiebreak Mode

random | Random

stopCountingAndAsk | Stop counting and ask

previousRoundCountsThenRandom | Previous Round Counts (then random)

prevousRoundCountsThenAsk | Previous Round Counts (then stop counting
and ask)

useCandidateOrder | Use candidate order in config file

generatePermutation | Generate permutation

randomSeed | Random Seed

numberOfWinners | Number of Winners

multiSeatBottomsUpPercentageThreshold | Percentage Threshold

nonIntegerWinningThreshold | Compute using HB Quota &gt;(Votes/ (Seats +
1))

hareQuota | Compute using Hare Quota =(Votes/Seats)

decimalPlacesForVoteArithmetic | Decimal Places for Vote Arithmetic
(Multi-Winner Only)

overvoteRule | Overvote Rule

alwaysSkipToNextRank | Always skip to next rank

ExhaustImmediately | Exhaust Immediately

exhaustIfMultipleContinuing | Exhaust if multiple continuing

maxSkippedRanksAllowed | How Many Consecutive Skipped Ranks Are Allowed

exhaustOnDuplicateCandidate | Exhaust on Multiple Ranks for the Same
Candidate

outputDirectory | Output Directory

tabulateByPrecinct | Tabulate by Precinct

generateCdfJson | Generate CDF JSON

<table>
<colgroup>
<col style="width: 16%" />
<col style="width: 17%" />
<col style="width: 37%" />
<col style="width: 28%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><strong>Document Revision History</strong></th>
</tr>
<tr class="odd">
<th><strong>Date</strong></th>
<th><strong>Version</strong></th>
<th><strong>Description</strong></th>
<th><strong>Author</strong></th>
</tr>
<tr class="header">
<th>09/28/2023</th>
<th>1.2.2</th>
<th><ul>
<li><p>Include automatic signature verification of Hart signed CVRs,
.hash output files and read-only output.</p></li>
<li><p>Output Directory instructions detail explicit steps to ensure
read-only permissions are set</p></li>
<li><p>Include directions to configure Verity Count to sign CVR
exports</p></li>
<li><p>Remove manual hash steps in ‘Generating Results Files’ header,
they are now done automatically</p></li>
</ul></th>
<th>Mathew Ruberg</th>
</tr>
<tr class="odd">
<th>04/28/2023</th>
<th>1.2.1</th>
<th>Updated to reflect RCTab v1.3.1</th>
<th>Kelly Sechrist</th>
</tr>
<tr class="header">
<th>04/04/2023</th>
<th>1.2.0</th>
<th>Updated to Reflect RCTab v.1.3.0</th>
<th>Rene Rojas</th>
</tr>
<tr class="odd">
<th>01/19/2022</th>
<th>1.1.0</th>
<th>Revisions for clarity</th>
<th>Ryan Kirby</th>
</tr>
<tr class="header">
<th>04/27/2021</th>
<th>1.0.0</th>
<th>System Operations Procedures</th>
<th>Chris Hughes</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

# 

