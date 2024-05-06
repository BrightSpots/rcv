# RCTab v1.3.2 Section 13 - Quality Assurance Plan v.1.2.2

> **RCTab v1.3.2 Section 13 - Quality Assurance Plan v.1.2.2** document
> is solely for use in the State of California. This document can be
> expanded or updated as is necessary or required. Where relevant, this
> document refers to specific sections and requirements of the
> California Voting System Standards. Any recommendations listed in this
> document should not supersede user jurisdiction procedures or other
> controlling governance entities.

## Scope

This document outlines the general Quality Assurance processes and
procedures of the RCTab counting software.

The RCTab software is designed for use as a round-by-round counting
software and installed on COTS equipment after centralization of the
cast vote record data. Centralization of data occurs based on the
policies, procedures, and laws of the jurisdiction using the counting
software. This software is designed specifically for use in ranked
choice voting elections.

The software is simple to install and utilize with proper training of
election officials. From the earliest concept of this product, RCTab was
designed to provide confidence and accuracy in the round-by-round count
of any jurisdiction. The software is designed for election official
input based on jurisdiction RCV rules. The software uses CVR data
exports from election tabulation equipment.

### Items Covered in the Scope

-   Requirements, design process, and definition of RCTab software.

-   Determination of the specifications that a COTS device must meet in
    order to optimize RCTab installation and operation.

-   The process recommendations for centralization of CVR data prior to
    round-by-round counting.

-   The validation and verification of the performance of RCTab.

-   Validation and verification of the process for installation of RCTab
    on COTS hardware along with the necessary steps to secure the system
    as would be required in a jurisdiction.

## Requirements, Design Process, and Definition of RCTab Software

### Requirements

### 

RCTab is tabulation software that is designed to use voting system data
(such as the Cast Vote Record (CVR) from a tabulation system) to run a
round-by-round tabulation of a ranked choice voting contest. The
software must be installed on a computer configured according to the
specifications listed in **<span class="mark">RCTab v1.3.2 Section 03 -
System Hardware Specification v.1.1.1.</span>**

The computer must be a standalone computer that is not connected to an
internet connection or a network of any kind. Wireless connection must
be disabled if available on the computer. All industry standard security
configurations must be set up and any security policies must be
followed, included but not limited to: computer hardening and
installation of a recommended antivirus software. For additional
information, see documentation listed below.

### Description of Parts and Materials Necessary for RCTab Use: (V.2: 2.12.2--Description) 

Trusted build of RCTab to be acquired from the Ranked Choice Voting
Resource Center or State Agency.

COTS computer that is not connected to the internet and is configured
according to all

specifications laid out in the following documents:

-   **<span class="mark">RCTab v1.3.2 Section 03 - System Hardware
    Specification v.1.1.1</span>**

-   **<span class="mark">RCTab v1.3.2 Section 07 - System Security
    Specification Requirements v.1.4.0</span>**

-   **<span class="mark">RCTab v1.3.2 Section 12 - Configuration
    Management Plan v.2.3.2</span>**

-   **<span class="mark">RCTab v1.3.2 Section 16 - System Hardening
    Procedures - Windows OS v.1.3.0</span>**

● Battery backup is also recommended if the facility does not have a
generator. This is not required to run the software.

### Ensuring Proper Functionality of the Parts and Materials

-   The parts were chosen in the following manner in accordance with
    VVSG Volume 2: 2:12.1 as it refers to Volume 1:8.5 and Volume 1:8.6.

    -   RCTab software--the voting tabulation product. (See below for
        all testing information regarding the product.)

    -   COTS hardware was chosen as an industry standard for a Windows
        machine. All set up should refer to the documentation listed
        above. RCVRC does not design, manufacture, or resale any
        hardware.

-   Test data storage process has been updated to meet the
    specifications outlined in the VVSG Volume 1:8.5c

## Design Process

The RCTab software is developed with the following tools, policies, and
practices to ensure robust software quality and reliability. RCTab
testing relates only to the function of the software and performs no
parts and materials testing as we produce only software and do not
design or manufacture hardware. Testing follows standards laid out in
the VVSG Volume 2:2.12.1 with regard to V1:8.5.

**<span class="mark">RCTab v1.3.2 Section 04 - System Functionality
Description v.1.1.1</span>** incorporates general functional
requirements for the tabulation software system. Requirements for
accuracy were taken into account when designing and performing all
testing including stress tests. The VVSG 2.0 requirements with regard to
accuracy were also utilized when designing and performing stress tests.
V2:2.12.1 also refers to V1:8.5, V1:8.6 and V1:8.7 and were followed
throughout the process. Regression testing is completed for each design
change or addition, whether minor or major. For additional information
about testing, refer to **<span class="mark">RCTab v1.3.2 Section 17 -
System Test and Verification Specification v.1.4.2</span>*.***

### Design, Testing, and QA Process Responsibilities

The design process and QA assessments are performed by a joint team that
includes the developers from Bright Spots and software team from
EARC/RCVRC. Project managers may be used on a contract basis to manage
QA responsibilities such as testing.

### Documentation of Quality Conformance Procedures

The RCTab development team uses a ticketing system to identify bugs as
well as design issues. All procedures below reflect this process and are
managed through the Resource Center and Bright Spots using GitHub. See
also **<span class="mark">RCTab v1.3.2 Section 12 - Configuration
Management Plan v.2.3.2</span>** to understand the process more fully.

### QA Processes and Procedures

*1) Version Control Software:*

We use Git version control software (git-scm.com) in conjunction with
Github (github.com/BrightSpots/rcv) to coordinate the efforts of our
developers and maintain a complete record of ALL software code changes
to the RCTab and the reasoning behind them.

*2) Software Revision Control Branching Policy:*

To isolate code in development from production code (released), we use
Git-Flow: a branching policy built on top of Git. The policy coordinates
development, testing, review, and deployment of code throughout the
development life-cycle. Details can be found here:
(www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow)

*3) Code reviews:*

All code changes submitted for incorporation into the RCTab software
must undergo a manual code review from at least one developer other than
the original drafter/developer. Other stakeholders and experts are
involved with code reviews as needed. Code reviews offer an additional
opportunity to identify potential defects, improve code structure,
clarity, performance, and robustness. Code merges are blocked until at
least one developer explicitly approves the code submission, at which
point the code is merged, and regression tests will be run. For code
review examples, see: github.com/BrightSpots/rcv/pulls.

*4) Regression Tests:*

We have developed a suite of 68 Tabulation Regression Tests. We continue
to add more tests as new features and bug fixes are added. These tests
are designed to verify that new code changes do not inadvertently alter
any tabulation results.

Each test has a set of inputs (just like any tabulation): A config file
and CVR file(s). Additionally, each test has an expected results file.
The automated test will: load the config, run the tabulation, verify
that the tabulation output matches the expected results. For actual test
data, see:
github.com/BrightSpots/rcv/tree/master/src/test/resources/network/brightspots/rcv/test\_data.

The names of the tests are mostly self-explanatory. For example,

*test\_set\_1\_exhaust\_at\_overvote* tests whether the tabulator
correctly interprets the overvoteRule = "exhaust immediately" setting.
Some of the tests aren't testing specific features but instead are
testing a known data set, e.g., *2018\_maine\_governor\_primary*. The
complete list is included below.

All regression tests are run by developers and must pass before any new
code can be merged into the tabulator. See Appendix A-1 for a list of
regression tests.

### Quality Conformance Inspections and Documentation 

*1) System Requirements Testing:*

Minimum system requirements were determined by repeated tabulation of
elections with 100,000 CVRs, 1,000,000 CVRs, and 6,000,000 CVRs on each
target platform. These tests were timed to ensure they complete in a
reasonable amount of time successfully.

*2) Ad-hoc user testing:*

When developing new features, we try to recruit as much user feedback
and user testing as possible. These testers are typically from the RCVRC
staff, manufacturer partners, election administrators, and RCV
activists.

*3) Process for Handling of deficiencies:*

Any defects discovered through testing or reported by users are recorded
and tracked using Github issue tracking tools. We confirm the existence
of the defect, evaluate its severity, and mark it accordingly. Depending
on user impact, development resources, and release timelines, we
schedule developers to address the defects in order of priority and then
do the actual work. Typically, this involves:

1.  reproducing the defect

2.  making necessary code changes to fix the defect on an isolated git
    branch 3) requesting a code review and implementing any changes
    arising from the code review 4) verifying that regression tests pass

3.  pushing the code change to the main branch and linking the issue in
    the commit message

4.  closing the issue and linking to the commit in Github issue tracker

In this way, we are able to ensure that all known issues are tracked,
and the most important ones are mitigated according to criticality. For
more details, see:

<u>https://github.com/BrightSpots/rcv/issues</u>.

### Testing Documentation

A timeline of testing that has occurred to date can be located in
**<span class="mark">RCTab v1.3.2 Section 17 - System Test and
Verification Specification v.1.4.2</span>.** We have updated general
procedures moving forward to include a test documentation log with
specific testing details including:

-   Test Date

-   Type and description of test performed

-   Location test performed

-   Individual/individuals who conducted the test

-   Test Outcomes

### 

### Regression Test Overview

We have developed a suite of 68 Tabulation Regression Tests and continue
to add more tests as new features and bug fixes are added. These tests
are designed to verify various aspects of Tabulator functionality behave
as expected. They also verify that new code changes do not inadvertently
alter Tabulator behavior. The entire test suite must be run, and all
tests must pass, before any new code changes can be merged into the main
Tabulator repository.

### Design Overview

Each test contains a set of normal tabulation inputs (config file and
cvr files) and a known valid "expected" results summary file. The test
runs a tabulation with new code, and compares the results of that
tabulation to previous, expected, correct results. In essence, we
isolate and analyze the effects any new code changes may have on the
tabulation output. This is a classic regression test design.

### Test Execution Details

The test suite will run through all tests automatically as follows:

1.  Tabulator is built from source code.

2.  For each test:

    1.  Run a tabulation using the test config file and cvr files.

    2.  Compare tabulation output summary file to reference expected
        summary file. If the files match exactly (except for timestamps)
        the test passes.

    3.  If the files do not match the test fails.

    4.  Test execution and test results are written to a log file and
        console.

### Testing Procedures

When new code is ready for submission a developer will follow these
procedures to ensure the code is safe for incorporation:

1\. Run test suite: in a console, from the rcv root directory, enter:
*./gradlew.bat test* 2. Observe the test output. If *any* test fails the
source of the failure must be identified and either:

a\. Fixed. Usually, a test failure is caused by a bug in logic or data
which can be fixed.

b\. Update the reference test asset. Sometimes bug fixes cause tests to
fail because they are now operating correctly. In these cases, test
output must be manually verified and peer reviewed (like any code
change) before it can be updated.

Example test outputs are included in:
TabulatorTests\_example\_console\_output.txt and
Test\_Results\_TabulatorTests.html

For actual test data, including configuration files and expected
results, see:
[<u>github.com/BrightSpots/rcv/tree/master/src/test/resources/network/brightspots/rcv/test\_data</u>](https://github.com/BrightSpots/rcv/tree/master/src/test/resources/network/brightspots/rcv/test_data).
To download the data, go to this page:
[<u>https://github.com/BrightSpots/rcv/tree/v1.3.2</u>](https://github.com/BrightSpots/rcv/tree/hotfix/1.3.2).
This is the location on GitHub where version 1.3.2 of the RCTab source
code is hosted. Click on “Code” and select “Download ZIP.” Once the ZIP
is downloaded, extract it. Navigate to the unzipped folder and navigate
to src/test/resources/network/brightspots/rcv/test\_data. All test data
files will be included in this folder.

Users can also manually run each test. Steps required to run tests
manually are as follows:

1.  Open RCTab

2.  Click “File”

3.  Click “Load”

4.  Navigate to the configuration file for the test you wish to run

5.  Select the configuration file and load it into RCTab

6.  Confirm that the configuration file properly loaded into RCTab by
    checking that the relevant fields are filled in

7.  Click “Tabulation”

8.  Click “Tabulate”

9.  Wait for RCTab to finish tabulating

10. Navigate to the output location for your results

11. Compare your results .json summary file to the .json summary file
    included with the test data. If these match exactly, the test
    passes.

### Additional Testing Procedures

In addition to regression testing of all changes, new features are
tested by developers and RCVRC staff to ensure that they work as
expected. If any features did not work as expected, a ticket was filed
on GitHub with the test data and an explanation of how the achieved
results differed from the expected results. Beyond issue tracking, RCVRC
and Bright Spots have not closely tracked these tests in the past and we
do not currently have a list of all tests run on the RCTab software.
Going forward, we will incorporate consistent test tracking protocols by
setting up an internal test tracking system that will capture all test
data, test results, and other required test specifications beyond just
regression testing.

In addition to past tests, the RCVRC and Bright Spots conducted scale
tests of RCTab using data provided by SLI during the testing process for
New York City certification. Tests included a contest with 100 CVR files
composed of 100,000 individual cast vote records each, a contest with
1,000 CVR files composed of 1,000,000 individual cast vote records each,
a contest with 6,000 CVR files composed of 6,000,000 individual cast
vote records each, and a set of 11 CVRs composed of 9,200,000 individual
cast vote records each. Volume tests with these records were conducted
throughout March and April 2021.

Additionally, see List of Tests at the end of this section.

Note: We are in the process of developing a more robust process for test
inspections, records, and documentation. This will address these
requirements in an accurate and reliable way.

### Product Documentation

All documentation in this submission is named according to the formula:

\[System Version\]\[Document/Section Number\] - \[Document Name\]
\[Document version\]

System Version: RCTab v1.3.2

Document version: 1.0.0

Example: RCTab v1.3.2 Section 13 - Quality Assurance Plan v.1.0.0

RCTab provides documentation in order to meet the VVSG standards
outlined in Volume 2, Section 2, Description of the Technical Data
Package. Refer to this reference list of documentation for more
information:

> ● **<span class="mark">RCTab v1.3.2 Section 01 - System Overview
> v.1.2.1</span>**
>
> **● <span class="mark">RCTab v1.3.2 Section 02 - Software Design and
> Specifications v.1.4.2</span>**
>
> **● <span class="mark">RCTab v1.3.2 Section 03 - System Hardware
> Specification v.1.1.1</span>**
>
> **● <span class="mark">RCTab v1.3.2 Section 04 - System Functionality
> Description v.1.1.1</span>**
>
> **● <span class="mark">RCTab v1.3.2 Section 07 - System Security
> Specification Requirements v.1.4.0</span>**
>
> **● <span class="mark">RCTab v1.3.2 Section 08 - System Operations
> Procedures v.1.2.2</span>**
>
> **● <span class="mark">RCTab v1.3.2 Section 09 - System Maintenance
> Manual v.1.3.2</span>**
>
> **● <span class="mark">RCTab v1.3.2 Section 10 - Personnel Deployment
> and Training v.1.2.1</span>**
>
> **● <span class="mark">RCTab v1.3.2 Section 12 - Configuration
> Management Plan v.2.3.2</span>**
>
> **● <span class="mark">RCTab v1.3.2 Section 13 - Quality Assurance
> Plan v.1.2.2</span>**
>
> **● <span class="mark">RCTab v1.3.2 Section 15 - System Change Notes
> v.1.3.1</span>**
>
> **● <span class="mark">RCTab v1.3.2 Section 16 - System Hardening
> Procedures - Windows OS v.1.3.0</span>**
>
> **● <span class="mark">RCTab v1.3.2 Section 17 - System Test and
> Verification Specification v.1.4.2</span>**
>
> **● <span class="mark">RCTab v1.3.2 Section 18 - User Guide
> v.1.2.2</span>**

**Appendix A-1**

## RCTab Regression Test Listing 

###  Tabulator Tests

> @DisplayName("NIST XML CDF 2")
>
> @DisplayName("unisyn\_xml\_cdf\_city\_tax\_collector")
>
> @DisplayName("unisyn\_xml\_cdf\_city\_mayor")
>
> @DisplayName("unisyn\_xml\_cdf\_city\_council\_member")
>
> @DisplayName("unisyn\_xml\_cdf\_city\_chief\_of\_police")
>
> @DisplayName("unisyn\_xml\_cdf\_city\_coroner")
>
> @DisplayName("unisyn\_xml\_cdf\_county\_sheriff")
>
> @DisplayName("unisyn\_xml\_cdf\_county\_coroner")
>
> @DisplayName("Clear Ballot - Kansas Primary")
>
> @DisplayName("Hart - Travis County Officers")
>
> @DisplayName("Hart - Cedar Park School Board")
>
> @DisplayName("Dominion test - Alaska test data")
>
> @DisplayName("Dominion test - Kansas test data")
>
> @DisplayName("Dominion test - Wyoming test data")
>
> @DisplayName("Dominion - No Precinct Data")
>
> @DisplayName("multi-cvr file dominion test")
>
> @DisplayName("test invalid params in config file")
>
> @DisplayName("test invalid source files")
>
> @DisplayName("2015 Portland Mayor")
>
> @DisplayName("2015 Portland Mayor Candidate Codes")
>
> @DisplayName("2013 Minneapolis Mayor Scale")
>
> @DisplayName("Continue Until Two Candidates Remain")
>
> @DisplayName("Continue Until Two Candidates Remain with Batch
> Elimination")
>
> @DisplayName("2017 Minneapolis Mayor")
>
> @DisplayName("2013 Minneapolis Mayor")
>
> @DisplayName("2013 Minneapolis Park")
>
> @DisplayName("2018 Maine Governor Democratic Primary")
>
> @DisplayName("testMinneapolisMultiSeatThreshold")
>
> @DisplayName("test for overvotes")
>
> @DisplayName("test excluding candidates in config file")
>
> @DisplayName("test minimum vote threshold setting")
>
> @DisplayName("test skipping to next candidate after overvote")
>
> @DisplayName("test Hare quota")
>
> @DisplayName("test sequential multi-seat logic")
>
> @DisplayName("test bottoms-up multi-seat logic")
>
> @DisplayName("test bottoms-up multi-seat with threshold logic")
>
> @DisplayName("test allow only one winner per round logic")
>
> @DisplayName("precinct example")
>
> @DisplayName("missing precinct example")
>
> @DisplayName("test tiebreak seed")
>
> @DisplayName("skipped first choice")
>
> @DisplayName("exhaust at overvote rule")
>
> @DisplayName("overvote skips to next rank")
>
> @DisplayName("skipped choice exhausts option")
>
> @DisplayName("skipped choice next option")
>
> @DisplayName("two skipped ranks exhausts option")
>
> @DisplayName("duplicate rank exhausts")
>
> @DisplayName("duplicate rank skips to next option")
>
> @DisplayName("multi-cdf tabulation")
>
> @DisplayName("multi-seat whole number threshold")
>
> @DisplayName("multi-seat fractional number threshold")
>
> @DisplayName("tiebreak using permutation in config")
>
> @DisplayName("tiebreak using generated permutation")
>
> @DisplayName("tiebreak using previousRoundCountsThenRandom")
>
> @DisplayName("treat blank as undeclared write-in")
>
> @DisplayName("undeclared write-in (UWI) cannot win test")
>
> @DisplayName("multi-seat UWI test")
>
> @DisplayName("overvote delimiter test")
>
> @DisplayName("sequential with batch elimination test")
>
> @DisplayName("sequential with continue until two test")
>
> @DisplayName("overvote exhaust if multiple continuing test")
>
> @DisplayName("no one meets minimum test")

### Security Tests

@DisplayName("Succeeds using the default, valid files")

@DisplayName("Verification fails when the signature is incorrect")

@DisplayName("Exception is thrown when the data file is modified")

> @DisplayName("Succeeds when data file is in a different folder (but
> has the right filename)")
>
> @DisplayName("Exception is thrown when the filenames differ")
>
> @DisplayName("Exception is thrown when the file is signed with an
> unsupported key")

## List of Tabulation Tests 

<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 22%" />
<col style="width: 26%" />
<col style="width: 37%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Name of Test</strong></th>
<th><strong>Name of Test Folder</strong></th>
<th><strong>Description of test</strong></th>
<th><strong>Purpose of Test</strong></th>
</tr>
<tr class="odd">
<th>RCVRC &amp; Bright Spots name for regression test</th>
<th><p><a
href="https://github.com/BrightSpots/rcv/tree/master/src/test/resources/network/brightspots/rcv/test_data"><u>Test
folder name in:</u></a></p>
<p>Refer to <mark>test_data.zip that includes all test data and was
submitted with documentation.</mark></p></th>
<th>Brief description of test and identification of RCTab
functionalities tested by test files.</th>
<th><p>Unless otherwise noted, each regression test can be used to test
control and data input/output, acceptance criteria, processing accuracy,
ballot interpretation logic, and production of audit trails and
statistical data.</p>
<p>Processes for exporting and handling data, under control and data
input, should also follow CVR handling procedures in a jurisdiction.</p>
<p>Security processes should be tested according to the requirements in
06 - System Security Specifications.</p>
<p>Exception handling tests are directly identified below.</p></th>
</tr>
<tr class="header">
<th>2013 Minneapolis Mayor</th>
<th>2013_minneapolis_mayor</th>
<th>Test the ability of RCTab to read and process ES&amp;S CVR files and
CVR settings usable with ES&amp;S CVR files. Test RCTab's ability to
properly count real-life RCV elections.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>2013 Minneapolis Mayor Scale</th>
<th>2013_minneapolis_mayor_scale</th>
<th>Tests the ability of RCTab to process a contest with 1,000,000
records. Test the ability of RCTab to read and process ES&amp;S CVR
files and CVR settings usable with ES&amp;S CVR files.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>2013 Minneapolis Park</th>
<th>2013_minneapolis_park</th>
<th>Test the ability of RCTab to read and process ES&amp;S CVR files and
CVR settings usable with ES&amp;S CVR files. Test the functionality of
the "multi-winner allow multiple winners per round" functionality.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>test bottoms-up multi-seat logic</th>
<th>2013_minneapolis_park_bottoms_up</th>
<th>Test the functionality of the bottoms-up winner election mode
setting. Test the ability of RCTab to read and process ES&amp;S CVR
files and CVR settings usable with ES&amp;S CVR files.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>2017 Minneapolis Park</th>
<th>2017_minneapolis_park</th>
<th>Test the ability of RCTab to read and process ES&amp;S CVR files and
CVR settings usable with ES&amp;S CVR files. Test the functionality of
the "multi-winner allow multiple winners per round" functionality.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>test Hare quota</th>
<th>2013_minneapolis_park_hare</th>
<th>Test the functionality of the Hare Quota Threshold Calculation Mode
setting. Note that while RCTab passes this test the Hare Quota
functionality has been updated but requires additional testing. Test the
functionality of the "multi-winner allow multiple winners per round"
functionality. Test the ability of RCTab to read and process ES&amp;S
CVR files and CVR settings usable with ES&amp;S CVR files.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>test sequential multi-seat logic</th>
<th>2013_minneapolis_park_sequential</th>
<th>Test the functionality of the multi-pass IRV winner election mode
setting. Test the ability of RCTab to read and process ES&amp;S CVR
files and CVR settings usable with ES&amp;S CVR files.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>2015 Portland Mayor</th>
<th>2015_portland_mayor</th>
<th>Test the ability of RCTab to read and process ES&amp;S CVR files and
CVR settings usable with ES&amp;S CVR files. Test ability to process a
single-winner RCV contest.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>2015 Portland Mayor Candidate Codes</th>
<th>2015_portland_mayor_codes</th>
<th>Test the ability of RCTab to read and process ES&amp;S CVR files and
CVR settings usable with ES&amp;S CVR files; ability to process a
single-winner RCV contest; ability to process a CVR using Candidate
Codes instead of Candidate Names</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>2017 Minneapolis Mayor</th>
<th>2017_minneapolis_mayor</th>
<th>Test the ability of RCTab to read and process ES&amp;S CVR files and
CVR settings usable with ES&amp;S CVR files, as well as RCTab's ability
to properly count real-life RCV elections.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>2018 Maine Governor Democratic Primary</th>
<th>2018_maine_governor_primary</th>
<th>Test the ability of RCTab to read and process ES&amp;S CVR files and
CVR settings usable with ES&amp;S CVR files, as well as RCTab's ability
to properly count real-life RCV elections.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>Clear Ballot - Kansas Primary</th>
<th>clear_ballot_kansas_primary</th>
<th>Tests the ability of RCTab to read and process Clear Ballot CVR
data</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>Continue Until Two Candidates Remain</th>
<th>continue_tabulation_test</th>
<th>Tests the ability of RCTab to properly count a single-winner contest
down to two final candidates</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>Continue Until Two Candidates Remain with Batch Elimination</th>
<th>continue_until_two_with_batch_elimination_test</th>
<th>Tests the ability of RCTab to count a single-winner contest using
both batch elimination and continue until two candidates remain settings
simultaneously.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>Dominion test - Alaska test data</th>
<th>dominion_alaska</th>
<th>Tests the ability of RCTab to read and process Dominion CVR data and
to run an RCV count on that data.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>Dominion test - Kansas test data</th>
<th>dominion_kansas</th>
<th>Tests the ability of RCTab to read and process Dominion CVR data and
to run an RCV count on that data.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>Dominion - Multi-File</th>
<th>dominion_multi_file</th>
<th>Tests the ability of RCTab to read and process Dominion CVR data
spread across multiple CVR files and to run an RCV count on that
data.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>Dominion - No Precinct Data</th>
<th>dominion_no_precinct_data</th>
<th>Tests the ability of RCTab to read and process Dominion CVR data and
to run an RCV count on that data.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>Dominion test - Wyoming test data</th>
<th>dominion_wyoming</th>
<th>Tests the ability of RCTab to read and process Dominion CVR data and
to run an RCV count on that data.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>test inactivating ballot after encountering duplicate ranking of
same candidate</th>
<th>duplicate_test</th>
<th>Tests the ability of RCTab to properly detect and inactivate a
ballot due to the same candidate being ranked multiple times on that
ballot.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>test excluding candidates in config file</th>
<th>excluded_test</th>
<th>Test the "Exclude" function in the Candidate settings of RCTab.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>Hart - Cedar Park School Board</th>
<th>hart_cedar_park_school_board</th>
<th>Tests the ability of RCTab to read and process Hart CVR data</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>Hart - Travis County Officers</th>
<th>hart_travis_county_officers</th>
<th>Tests the ability of RCTab to read and process Hart CVR data</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>test invalid params in config file</th>
<th>invalid_params_test</th>
<th>Tests the ability of RCTab show errors if a configuration file is
incomplete or improperly filled out.</th>
<th>Exception handling</th>
</tr>
<tr class="header">
<th>test invalid source files</th>
<th>invalid_sources_test</th>
<th>Tests the ability of RCTab to show errors if CVR files are
incompatible</th>
<th>Exception handling</th>
</tr>
<tr class="odd">
<th>test minimum vote threshold setting</th>
<th>minimum_threshold_test</th>
<th>Test the "minimum vote threshold" setting in winner election
mode.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>testMinneapolisMultiSeatThreshold</th>
<th>minneapolis_multi_seat_threshold</th>
<th>Test the ability of RCTab to determine winning candidates according
to the default Threshold Calculation method in multi-winner elections.
Test the functionality of the "multi-winner allow multiple winners per
round" functionality.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>missing precinct example</th>
<th>missing_precinct_example</th>
<th>Test the ability of RCTab to continue tabulating if "Precinct Column
ID" is supplied but precinct information is missing in part of a CVR
file.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>test bottoms-up multi-seat with threshold logic</th>
<th>multi_seat_bottoms_up_with_threshold</th>
<th>Test the functionality of the bottoms-up using percentage threshold
winner election mode setting.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>multi-seat UWI test</th>
<th>multi_seat_uwi_test</th>
<th>Undeclared write-ins should not win elections. This test ensures
that RCTab properly handles this exception by not awarding a win to an
undeclared write-in candidate in a multi-winner contest.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>NIST XML CDF 2</th>
<th>nist_xml_cdf_2</th>
<th>Tests the ability of RCTab to read and process CDF data in XML
format</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>No candidates meet minimum vote threshold</th>
<th>no_candidates_meet_minimum</th>
<th>Tests the ability of RCTab to provide an accurate error message if
all candidates fall below the value set in the Minimum Vote Threshold
field.</th>
<th>Exception handling.</th>
</tr>
<tr class="header">
<th>precinct example</th>
<th>precinct_example</th>
<th>Test the ability of RCTab to detect precinct information based on
CVR file data and the ability to produce precinct results using the
Tabulate by Precinct functionality. Note that Tabulate by Precinct
function produces precinct-level results of the RCV contest but does not
identify precinct-level winners. This functionality needs updating to
identify in-precinct winners.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>sample interactive tiebreak</th>
<th>sample_interactive_tiebreak</th>
<th>Test the functionality of the interactive tiebreak function
available in "Stop counting and ask," or "Previous round counts (then
stop counting and ask)" tiebreaking modes. This is not a regression test
as it requires user input. It is included in the "sample_input" folder
of RCTab installation folder as an additional test of RCTab.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>Sequential with batch</th>
<th>sequential_with_batch</th>
<th>Tests the ability of RCTab to properly batch eliminate candidates in
a multi-pass IRV tabulation scenario.</th>
<th></th>
</tr>
<tr class="odd">
<th>Sequential with continue until two</th>
<th>sequential_with_continue_until_two</th>
<th>Tests the ability of RCTab to eliminate down to the final two
candidates in a multi-pass IRV tabulation scenario.</th>
<th></th>
</tr>
<tr class="header">
<th>test skipping to next candidate after overvote</th>
<th>skip_to_next_test</th>
<th>Test the functionality of the "Always skip to next rank" overvote
setting.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>skipped first choice</th>
<th>test_set_0_skipped_first_choice</th>
<th>Test the ability of RCTab to properly process CVR data with a
skipped first ranking. Test the ability of RCTab to export CVR data in
the CDF.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>exhaust at overvote rule</th>
<th>test_set_1_exhaust_at_overvote</th>
<th>Test the functionality of the "Exhaust immediately" overvote
setting. Test the ability of RCTab to export CVR data in the CDF.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>overvote skips to next rank</th>
<th>test_set_2_overvote_skip_to_next</th>
<th>Test the functionality of the "Always skip to next rank" overvote
setting. Test the ability of RCTab to export CVR data in the CDF.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>skipped choice exhausts option</th>
<th>test_set_3_skipped_choice_exhaust</th>
<th>Test the functionality of the "how many consecutive skipped ranks
are allowed" setting when ballots exhaust after a single skipped
ranking. Test the ability of RCTab to export CVR data in the CDF.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>skipped choice next option</th>
<th>test_set_4_skipped_choice_next</th>
<th>Test the functionality of the "how many consecutive skipped ranks
are allowed" setting when ballots don't exhaust after skipped rankings.
Test the ability of RCTab to export CVR data in the CDF.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>two skipped ranks exhausts option</th>
<th>test_set_5_two_skipped_choice_exhaust</th>
<th>Test the functionality of the "how many consecutive skipped ranks
are allowed" setting when ballots exhaust after multiple skipped
rankings. Test the ability of RCTab to export CVR data in the CDF.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>duplicate rank exhausts</th>
<th>test_set_6_duplicate_exhaust</th>
<th>Test the functionality of the "Exhaust on multiple ranks for the
same candidate" function, if function is turned on - ballots should
exhaust when a duplicate ranking is encountered. Test the ability of
RCTab to export CVR data in the CDF.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>duplicate rank skips to next option</th>
<th>test_set_7_duplicate_skip_to_next</th>
<th>Test the functionality of the "Exhaust on multiple ranks for the
same candidate" function, if the function is turned off - RCTab will
ignore duplicate candidate rankings. Test the ability of RCTab to export
CVR data in the CDF.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>multi-cdf tabulation</th>
<th>test_set_8_multi_cdf</th>
<th>Tests the ability of RCTab to read and process multiple CDF data
files in XML format. Test the ability of RCTab to export CVR data in the
CDF.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>test allow only one winner per round logic</th>
<th>test_set_allow_only_one_winner_per_round</th>
<th>Test the functionality of the multi-winner allows only one winner
per round logic.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>multi-seat fractional number threshold</th>
<th>test_set_multi_winner_fractional_threshold</th>
<th>Test the functionality of the HB Quota Threshold Calculation Mode
setting. Test the ability of RCTab to export CVR data in the CDF.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>multi-seat whole number threshold</th>
<th>test_set_multi_winner_whole_threshold</th>
<th>Test the functionality of the default Threshold Calculation Mode
setting. Test the ability of RCTab to export CVR data in the CDF.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>overvote delimiter test</th>
<th>test_set_overvote_delimiter</th>
<th>Test the functionality of the "overvote delimiter" setting.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>treat blank as undeclared write-in</th>
<th>test_set_treat_blank_as_undeclared_write_in</th>
<th>Test the functionality of the "Treat Blank as Undeclared Write-In"
setting for CVRs.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>tiebreak using generated permutation</th>
<th>tiebreak_generate_permutation_test</th>
<th>Test the functionality of the "Generate permutation" tiebreak
setting.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>tiebreak using previousRoundCountsThenRandom</th>
<th>tiebreak_previous_round_counts_then_random_test</th>
<th>Test the functionality of the "Previous Round Counts then Random"
tiebreak setting.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>test tiebreak seed</th>
<th>tiebreak_seed_test</th>
<th>Test the functionality of the "random seed" setting required for any
of the Random Tiebreak modes ("Random" "Previous Round Counts then
Random" and "Generate Permutation")</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>tiebreak using permutation in config</th>
<th>tiebreak_use_permutation_in_config</th>
<th>Test the functionality of the "Use candidate order in config file"
tiebreak setting.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>unisyn_xml_cdf_city_chief_of_police</th>
<th>unisyn_xml_cdf_city_chief_of_police</th>
<th>Tests the ability of RCTab to read and process CDF data in XML
format</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>unisyn_xml_cdf_city_coroner</th>
<th>unisyn_xml_cdf_city_coroner</th>
<th>Tests the ability of RCTab to read and process CDF data in XML
format</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>unisyn_xml_cdf_city_council_member</th>
<th>unisyn_xml_cdf_city_council_member</th>
<th>Tests the ability of RCTab to read and process CDF data in XML
format</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>unisyn_xml_cdf_city_mayor</th>
<th>unisyn_xml_cdf_city_mayor</th>
<th>Tests the ability of RCTab to read and process CDF data in XML
format</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>unisyn_xml_cdf_city_tax_collector</th>
<th>unisyn_xml_cdf_city_tax_collector</th>
<th>Tests the ability of RCTab to read and process CDF data in XML
format</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>unisyn_xml_cdf_county_coroner</th>
<th>unisyn_xml_cdf_county_coroner</th>
<th>Tests the ability of RCTab to read and process CDF data in XML
format</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="odd">
<th>unisyn_xml_cdf_county_sheriff</th>
<th>unisyn_xml_cdf_county_sheriff</th>
<th>Tests the ability of RCTab to read and process CDF data in XML
format</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
<tr class="header">
<th>undeclared write-in (UWI) cannot win test</th>
<th>uwi_cannot_win_test</th>
<th>Undeclared write-ins (UWIs) cannot win elections in RCTab
tabulation. This test ensures that RCTab properly handles this exception
by not awarding a win to an undeclared write-in candidate in a
single-winner contest.</th>
<th><p>Control and data input/output;</p>
<p>Processing accuracy;</p>
<p>Ballot interpretation logic;</p></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## List of Security Tests

<table>
<colgroup>
<col style="width: 17%" />
<col style="width: 33%" />
<col style="width: 48%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Name of Test</strong></th>
<th><strong>Description of test</strong></th>
<th><strong>Purpose of Test</strong></th>
</tr>
<tr class="odd">
<th>RCVRC &amp; Bright Spots name for regression test</th>
<th>Brief description of test and identification of RCTab
functionalities tested by test files.</th>
<th><p>Unless otherwise noted, each regression test can be used to test
control and data input/output, acceptance criteria, processing accuracy,
ballot interpretation logic, and production of audit trails and
statistical data.</p>
<p>Processes for exporting and handling data, under control and data
input, should also follow CVR handling procedures in a jurisdiction.</p>
<p>Security processes should be tested according to the requirements in
06 - System Security Specifications.</p>
<p>Exception handling tests are directly identified below.</p></th>
</tr>
<tr class="header">
<th>Succeeds using the default, valid files</th>
<th>Tests that properly signed Hart CVRs succeed cryptographic
validation</th>
<th>Ensuring cryptographic validation of signed Hart CVRs is implemented
correctly</th>
</tr>
<tr class="odd">
<th>Verification fails when the signature is incorrect</th>
<th>When the cryptographic signature of the Hart CVR is not valid,
confirm that it successfully throws an exception</th>
<th>Ensuring cryptographic validation of signed Hart CVRs is implemented
correctly</th>
</tr>
<tr class="header">
<th>Exception is thrown when the data file is modified</th>
<th>Confirms that if the signed Hart CVR file itself is modified that
signature validation should fail</th>
<th>Ensuring cryptographic validation of signed Hart CVRs is implemented
correctly</th>
</tr>
<tr class="odd">
<th>Succeeds when data file is in a different folder (but has the right
filename)</th>
<th>Hart includes a path in the data that is signed. Since CVRs will be
moved from their original location to the RCTab machine, we make sure
that this doesn’t affect signature verification.</th>
<th>Ensuring cryptographic validation of signed Hart CVRs is implemented
correctly</th>
</tr>
<tr class="header">
<th>Exception is thrown when the filenames differ</th>
<th>Though files can move, we require that file names stay the
same.</th>
<th>Ensuring cryptographic validation of signed Hart CVRs is implemented
correctly</th>
</tr>
<tr class="odd">
<th>Exception is thrown when the file is signed with an unsupported
key</th>
<th>When the public key is not a valid public key, ensure that signature
verification fails</th>
<th>Ensuring cryptographic validation of signed Hart CVRs is implemented
correctly</th>
</tr>
<tr class="header">
<th>Ensure FIPS Compliance check is run</th>
<th>Tests that Java Security Providers are properly setup to ensure FIPS
compliance</th>
<th>Ensuring cryptographic validation of signed Hart CVRs is implemented
correctly</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

**Document Revision History**

<table>
<colgroup>
<col style="width: 16%" />
<col style="width: 12%" />
<col style="width: 48%" />
<col style="width: 22%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Date</strong></th>
<th><strong>Version</strong></th>
<th><strong>Description</strong></th>
<th><strong>Author</strong></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>11/09/2023</p>
</blockquote></th>
<th><blockquote>
<p>1.2.2</p>
</blockquote></th>
<th><blockquote>
<p>Adds security tests for v1.3.2</p>
</blockquote></th>
<th><blockquote>
<p>Mathew Ruberg</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>04/27/2023</p>
</blockquote></th>
<th><blockquote>
<p>1.2.1</p>
</blockquote></th>
<th><blockquote>
<p>Updated to reflect RCTab v1.3.1</p>
</blockquote></th>
<th><blockquote>
<p>Kelly Sechrist</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>04/04/2023</p>
</blockquote></th>
<th><blockquote>
<p>1.2.0</p>
</blockquote></th>
<th><blockquote>
<p>Updated to Reflect V.1.3.0</p>
</blockquote></th>
<th><blockquote>
<p>Rene Rojas</p>
</blockquote></th>
</tr>
<tr class="header">
<th><blockquote>
<p>05/09/2021</p>
</blockquote></th>
<th><blockquote>
<p>1.1.0</p>
</blockquote></th>
<th><blockquote>
<p>Updates to scope of work, reference documents, and parts and
Materials</p>
</blockquote></th>
<th><blockquote>
<p>Kelly Sechrist</p>
</blockquote></th>
</tr>
<tr class="odd">
<th><blockquote>
<p>05/02/2021</p>
</blockquote></th>
<th><blockquote>
<p>1.0.0</p>
</blockquote></th>
<th><blockquote>
<p>Quality Assurance Plan</p>
</blockquote></th>
<th><blockquote>
<p>Chris Hughes</p>
</blockquote></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

# 

