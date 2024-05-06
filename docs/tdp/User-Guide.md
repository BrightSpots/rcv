# RCTab v1.3.2 Section 17 - System Test and Verification Specification v.1.4.2

> **RCTab v1.3.2 Section 17 - System Test and Verification Specification
> v.1.4.2** document is solely for use in the State of California. This
> document can be expanded or updated as is necessary or required. Where
> relevant, this document refers to specific sections and requirements
> of the California Voting System Standards. Any recommendations listed
> in this document should not supersede user jurisdiction procedures or
> other controlling governance entities.

## 9.7 System Test and Verification Specification 

The manufacturer shall provide test and verification specifications for:
Development test specifications.

### 9.7.1 Development Test Specifications 

a\. The manufacturer shall describe the plans, procedures, and data used
during software development and system integration to verify system
logic correctness, data quality, and security.

b\. This description shall include: Test identification and design,
including:

i\. Test structure

ii\. Test sequence or progression; and

iii\. Test conditions

#### Test Structure

Included below is the basic template used to define every setting for
each test set used to test RCTab. This form is filled out with the
settings for the relevant test. Testers can compare the information in
this form to the information in the configuration file loaded from the
relevant folder in test\_data to ensure that all settings are correct.
Each setting selected in this form has been tested. RCTab has not yet
created a Test Structure Form for every RCTab test but will set up a
form for all RCTab regression tests based on this form. Questions about
this form can be made to the Ranked Choice Voting Resource Center at
info@rcvresources.org or by calling 1-833-868-3728. We also have
included an example Test Structure Form filled out according to the
requirements of the Portland 2015 Mayor test.

#### Regression Test Overview

We have developed a suite of 68 Tabulation Regression Tests and continue
to add more tests as new features and bug fixes are added. These are
designed to verify that various aspects of Tabulator functionality
behave as expected. They also verify that new code changes do not
inadvertently alter Tabulator behavior. The entire test suite must be
run, and all tests must pass before any new code changes can be merged
into the main Tabulator repository.

#### Design Overview

Each test contains a set of normal tabulation inputs (config file and
cvr files) and a known valid "expected" results summary file. The test
runs a tabulation with new code, and compares the results of that
tabulation to previous, expected, correct results. In essence, we
isolate and analyze the effects any new code changes may have on the
tabulation output. This is a classic regression test design.

#### Test Execution Details

The test suite will run through all tests automatically as follows:

1.  Tabulator is built from source code.

2.  For each test:

    1.  Run a tabulation using the test config file and cvr files.

    2.  Compare tabulation output summary file to reference expected
        summary file.

    3.  If the files match exactly (except for timestamps) the test
        passes.

    4.  If the files do not match the test fails.

    5.  Test execution and test results are written to a log file and
        console.

#### Test Conditions

Test conditions require the following files for input for each test
case: Configuration file (JSON), Cast Vote Record (typically CSV
format). Confirm appropriate files are uploaded to proceed with
appropriate testing conditions.

For more information on the development process as it relates to testing
see **<span class="mark">RCTab v1.3.2 Section 13 - Quality Assurance
Plan v.1.2.2</span>**.

c\. Standard test procedures, including any assumptions or constraints

#### Testing Procedures

When new code is ready for submission, a developer will follow these
procedures to ensure the code is safe for incorporation:

1.  Run test suite: in a console, from the rcv root directory, enter:
    *./gradlew.bat test*

2.  Observe the test output. If *any* test fails the source of the
    failure must be identified and either:

    1.  Fixed. Usually, a test failure is caused by a bug in logic or
        data which can be fixed.

    2.  Update the reference test asset. Sometimes bug fixes cause tests
        to fail because they are now operating correctly. In these
        cases, test output must be manually verified and peer-reviewed
        (like any code change) before it can be updated.

Example test outputs are included in:
TabulatorTests\_example\_console\_output.txt and
Test\_Results\_TabulatorTests.html

For actual test data, including configuration files and expected
results, refer to <span class="mark">test\_data.zip that includes all
test data and was submitted with documentation.</span>

Users can also manually run each test. The steps required to run tests
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
    included with the data. If this matches exactly, the test passes.

#### Additional Testing Procedures

In addition to regression testing of all changes, new features are
tested by developers and RCVRC staff to ensure that they work as
expected. If any features did not work as expected, a ticket was filed
on GitHub with the test data and an explanation of how the achieved
results differed from the expected results.

The RCVRC and Bright Spots also conduct scale tests of the RCTab. Tests
include a contest with 100 CVR files composed of 100,000 individual cast
vote records each, a contest with 1,000 CVR files composed of 1,000,000
individual cast vote records each, a contest with 6,000 CVR files
composed of 6,000,000 individual cast vote records each, and a set of 11
CVRs composed of 9,200,000 individual cast vote records each. Volume
tests with these records were conducted throughout March and April 2021.

d\. Special purpose test procedures including any assumptions or
constraints;

No special-purpose test procedures are followed.

e\. Test data, test data source, whether it is real or simulated, and
how test data are controlled;

#### Test Data

The data for 2017 Minneapolis Mayor, 2013 Minneapolis Mayor, 2013
Minneapolis Park, 2018 Maine Governor Democratic Primary are all from
real-world elections and were procured directly from the websites of the
jurisdictions listed in the name of the data. All other data was
manufactured by Bright Spots developers, RCVRC staff, voting system
vendors, or election jurisdictions. Any data with vendor names included
(Unisyn, Clear Ballot, Dominion, Hart, CDF) was procured from vendors
directly or from jurisdictions working with those vendors. Any data not
falling in those categories was manufactured by RCTab developers
according to the CVR requirements of the software to test specific
functionalities of the RCTab software.

Regression test data are controlled by hosting them on GitHub and
updating tests as new features are added to RCTab. Other test data, for
tests run by RCVRC staff, Bright Spots developers, or others, did not
previously have clear controls on data. Data was procured from trusted
sources, such as actual election jurisdictions and voting system
vendors, as much as possible or generated according to the
specifications of previously used CVR data from vendors. However, no
formal controls were in place for test data or for tracking results.
Results were communicated via the relevant issue(s) on GitHub. We are
implementing formal control methods for tests and test data going
forward.

f\. Expected test results;

Expected results for regression tests are included in the test folders
available in the <span class="mark">test\_data.zip that includes all
test data and was submitted with documentation.</span>

g\. Criteria for evaluating test results.

The names of the tests are mostly self-explanatory. For example,
*test\_set\_1\_exhaust\_at\_overvote* tests whether the tabulator
correctly interprets the overvoteRule = "exhaust immediately" setting.
Some of the tests aren't testing specific features but instead are
testing a known data set, e.g., *2018\_maine\_governor\_primary*. The
complete list is included below.

Results from any tests run on regression test data should 100% match the
expected results in each individual test’s folder on GitHub. Note that
test folders include only .json summary result files, while tests run by
users will produce .json summary results, .csv summary results, and .log
audit log files. The .json summary results should therefore be compared
when evaluating test results.

In tests run by RCVRC staff and Bright Spots developers when
incorporating new features, any features were tested according to
requirements as laid out in RCV laws or regulations being incorporated
into RCTab.

### 9.7.2 Test Specifications 

The manufacturer shall provide specifications for verification and
validation of overall software performance. These specifications shall
cover:

a\. Control and data input/output;

-   Data input: Data used in RCTab should come directly from the user
    jurisdiction certified voting system. When exporting the data from
    the certified voting system the users should adhere to all export
    procedures as outlined by the voting system vendor.

<!-- -->

-   Data output: Data created via RCTab should be created using the
    guidelines and procedures outlined in **<span class="mark">RCTab
    v1.3.2 Section 18 - User Guide v.1.2.2</span>**.

b\. Acceptance criteria;

-   Tests in the set of regression tests are designed to test one or
    more functionalities of RCTab. When each test is performed, the
    results of the test will reveal whether RCTab meets the
    functionality described in the TDP. Information about how each
    functionality of RCTab is intended to function is provided in
    **<span class="mark">RCTab v1.3.2 Section 02 - Software Design and
    Specifications v.1.4.2</span>** and **<span class="mark">RCTab
    v1.3.2 Section 18 - User Guide v.1.2.2</span>***.*

c\. Processing accuracy;

-   All functions tested must produce a result that matches the expected
    results. Matching results means a test passed this requirement.
    Processing accuracy procedures are outlined in
    **<span class="mark">RCTab v1.3.2 Section 18 - User Guide
    v.1.2.2</span>**.

d\. These specifications shall cover: Data quality assessment and
maintenance;

-   Data quality is tested by ensuring that data used in RCTab comes
    directly from the user jurisdiction certified voting system and by
    producing expected results for a test before running a test itself.
    Once a test is run, results should be inspected to confirm that they
    match the expected outcome. Data should be maintained on secure
    drives or secured computers/networks, as required by the user
    jurisdiction.

e\. Ballot interpretation logic;

-   Regression tests can be used to test ballot interpretation logic.
    All CVR data will include discrete and clearly defined values for
    how each ranking on an RCV ballot was used. Ballot interpretation
    via RCTab relies upon the candidate names, winning election rules,
    and voter error rules a user specifies in a configuration file. If
    the produced results match the expected results, then a test passed
    the ballot interpretation logic requirement.

f\. Exception handling;

-   The invalid\_params\_test and invalid\_sources\_test can be used to
    test exception handling in RCTab. When run through RCTab these tests
    will cause RCTab to produce errors in the operator log box at the
    bottom of the user interface. If those errors shown match those in
    the below screenshot, the test was successful.

<img src="../media/image10.png"
style="width:6.5in;height:1.05556in" />

g\. Security;

-   RCTab has no tests specifically designed to test security. RCTab
    relies on a jurisdiction’s security policies, as laid out in
    **<span class="mark">RCTab v1.3.2 Section 06 - System Design
    Specifications v.1.1.1</span>**. Security can be maintained by
    ensuring that all security procedures follow the requirements set
    out in **<span class="mark">RCTab v1.3.2 Section 06 - System Design
    Specifications v.1.1.1</span>**.

h\. Production of audit trails and statistical data.

-   Regression tests will produce .log audit files and will log general
    information about the contest tabulation to the operator .log file.
    Production of these files can be tested using any regression test
    files, so long as the user activates the “Tabulate” function. All
    tests that produce a successful tabulation will also create .csv
    summary results files and .json summary results files. If these .log
    files are generated and updated, and .csv and .json summary files
    are produced when RCTab successfully completes a tabulation, then
    this requirement passes.

The specifications shall identify procedures for assessing and
demonstrating the suitability of the software for election use.

-   For additional user jurisdiction testing specifications, please see
    **<span class="mark">RCTab v1.3.2 Section 05 - Acceptance Test
    Procedures v.1.2.1</span>** and **<span class="mark">RCTab v1.3.2
    Section 11 - L&A Testing v.1.3.2</span>** to review manufacturer
    procedures to ensure the user jurisdiction has received and is using
    the correct trusted build for the state of California.

-   **<span class="mark">RCTab v1.3.2 Section 18 - User Guide
    v.1.2.2</span>** also includes a detailed guide to expected user
    operation of RCTab, which can be used to create tests of the user
    interface, tabulation functionalities, and other functionalities of
    RCTab software. The manufacturer is available to support development
    of additional tests.

## RCTab Test Structure Form

<table>
<colgroup>
<col style="width: 7%" />
<col style="width: 7%" />
<col style="width: 15%" />
<col style="width: 1%" />
<col style="width: 18%" />
<col style="width: 6%" />
<col style="width: 14%" />
<col style="width: 28%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="8"><strong>RCTab Test Structure Form</strong></th>
</tr>
<tr class="odd">
<th colspan="8" rowspan="3">This form is the basic template used to
define every test setting for each test set used to test RCTab. This
form is filled out with the settings for the [TEST NAME]. Testers can
compare the information in this form to the information in the
configuration file loaded from the relevant folder in test_data to
ensure that all settings are correct. Each setting selected in this form
is a setting tested by this test. RCTab has not yet created a Test
Structure Form for every RCTab test, but will set up a form for all
RCTab regression tests based on this form. Questions about this form can
be made to the Ranked Choice Voting Resource Center at
info@rcvresources.org or by calling 1-833-868-3728.</th>
</tr>
<tr class="header">
</tr>
<tr class="odd">
</tr>
<tr class="header">
<th colspan="8"></th>
</tr>
<tr class="odd">
<th colspan="8"><strong>Contest Info</strong></th>
</tr>
<tr class="header">
<th colspan="4">Contest Name*:</th>
<th colspan="2"></th>
<th>Contest Date:</th>
<th></th>
</tr>
<tr class="odd">
<th colspan="4">Contest Jurisdiction:</th>
<th colspan="2"></th>
<th>Contest Office:</th>
<th></th>
</tr>
<tr class="header">
<th colspan="4">Rules Description:</th>
<th colspan="4"></th>
</tr>
<tr class="odd">
<th colspan="8"><strong>CVR Files</strong></th>
</tr>
<tr class="header">
<th colspan="2">Provider*:</th>
<th colspan="3"></th>
<th colspan="3"><em>Enter Vendor (ES&amp;S, Hart, etc.)</em></th>
</tr>
<tr class="odd">
<th colspan="2">File Path*:</th>
<th colspan="3"></th>
<th colspan="3"><em>Location of the cvr export file.</em></th>
</tr>
<tr class="header">
<th colspan="2">First Vote Column Index*:</th>
<th colspan="3"></th>
<th colspan="3"><em>Enter the Column where the First Vote is in the CVR
export file.</em></th>
</tr>
<tr class="odd">
<th colspan="2">First Vote Row Index*:</th>
<th colspan="3"></th>
<th colspan="3"><em>Enter the Row where the First Vote is in the CVR
export file.</em></th>
</tr>
<tr class="header">
<th colspan="2">ID Column Index:</th>
<th colspan="3"></th>
<th colspan="3"><em>Enter the ID Column (if being used)</em></th>
</tr>
<tr class="odd">
<th colspan="2">Precinct Column Index:</th>
<th colspan="3"></th>
<th colspan="3"><em>Enter the Precinct Column in the CVR export
file.</em></th>
</tr>
<tr class="header">
<th colspan="2">Overvote Label</th>
<th colspan="3"></th>
<th colspan="3"></th>
</tr>
<tr class="odd">
<th colspan="2">Undervote Label</th>
<th colspan="3"></th>
<th colspan="3"></th>
</tr>
<tr class="header">
<th colspan="2">Undeclared Write-In Label</th>
<th colspan="3"></th>
<th colspan="3"><em>User will define. Must match case and spelling with
CVR</em></th>
</tr>
<tr class="odd">
<th colspan="2">Treat Blank as Undeclared WI</th>
<th colspan="3"></th>
<th colspan="3"><em>Circle One</em></th>
</tr>
<tr class="header">
<th colspan="8"><strong>Candidates</strong></th>
</tr>
<tr class="odd">
<th>Name*:</th>
<th colspan="2"></th>
<th colspan="5"><em>Obtain a list of candidates from the EMS System
(Attach the list of candidates to this form)</em></th>
</tr>
<tr class="header">
<th>Code:</th>
<th colspan="2"></th>
<th colspan="5"><em>Enter the Code for each candidate (if being used)
example: DDE</em></th>
</tr>
<tr class="odd">
<th>Excluded:</th>
<th colspan="2"></th>
<th colspan="5"><em>Check box if a candidate is not being counted in
this tabulation</em></th>
</tr>
<tr class="header">
<th colspan="8"><strong>Winning Rules</strong></th>
</tr>
<tr class="odd">
<th colspan="4">Winner Election Mode*</th>
<th colspan="2"></th>
<th colspan="2" rowspan="6">Any user jurisdiction guidelines which
identify the specific rules for ranked choice voting elections should be
attached to this form for easy reference. Please see the RCTab User
Guide for more information about the available selections that can be
made.</th>
</tr>
<tr class="header">
<th colspan="4">Maximum # of Ranked Candidates*</th>
<th colspan="2"></th>
</tr>
<tr class="odd">
<th colspan="4">Minimum Vote Threshold</th>
<th colspan="2"></th>
</tr>
<tr class="header">
<th colspan="4">Use Batch Elimination</th>
<th colspan="2">Y N (Circle One)</th>
</tr>
<tr class="odd">
<th colspan="4">Continue until Two Candidates Remain</th>
<th colspan="2">Y N (Circle One)</th>
</tr>
<tr class="header">
<th colspan="4">Tiebreak Mode*</th>
<th colspan="2"></th>
</tr>
<tr class="odd">
<th colspan="4">Random Seed*</th>
<th colspan="2"></th>
<th colspan="2" rowspan="8">Any user jurisdiction guidelines which
identify the specific rules for ranked choice voting elections should be
attached to this form for easy reference. Please see the RCTab User
Guide for more information about the available selections that can be
made.</th>
</tr>
<tr class="header">
<th colspan="4">Number of Winners*</th>
<th colspan="2"></th>
</tr>
<tr class="odd">
<th colspan="4">Percentage Threshold*</th>
<th colspan="2"></th>
</tr>
<tr class="header">
<th colspan="4">Threshold Calculation Method*</th>
<th colspan="2"></th>
</tr>
<tr class="odd">
<th colspan="4">Most Common Threshold</th>
<th colspan="2"></th>
</tr>
<tr class="header">
<th colspan="4">HB Quota</th>
<th colspan="2"></th>
</tr>
<tr class="odd">
<th colspan="4">Hare Quota‡</th>
<th colspan="2"></th>
</tr>
<tr class="header">
<th colspan="4">Decimal Places for Vote Arith (MW ONLY)*</th>
<th colspan="2"></th>
</tr>
<tr class="odd">
<th colspan="8"><strong>Voter Error Rules</strong></th>
</tr>
<tr class="header">
<th colspan="4">Overvote Rule*</th>
<th colspan="2"></th>
<th colspan="2">Choose one of the three available options.</th>
</tr>
<tr class="odd">
<th colspan="4">Skip to next rank</th>
<th colspan="2"></th>
<th colspan="2"></th>
</tr>
<tr class="header">
<th colspan="4">Exhaust Immediately</th>
<th colspan="2"></th>
<th colspan="2"></th>
</tr>
<tr class="odd">
<th colspan="4">Exhaust if mult. cont.</th>
<th colspan="2"></th>
<th colspan="2"></th>
</tr>
<tr class="header">
<th colspan="4">Consecutive Skip Ranks Allowed</th>
<th colspan="2"></th>
<th colspan="2">Enter the number of consecutive skipped rankings or
check Unlimited if permitted.</th>
</tr>
<tr class="odd">
<th colspan="4">Exhaust on multi ranks for same candidate</th>
<th colspan="2">Y N (Circle One)</th>
<th colspan="2">Check if multiple ranks for the same candidate are not
permitted.</th>
</tr>
<tr class="header">
<th colspan="8"><strong>Output</strong></th>
</tr>
<tr class="odd">
<th colspan="8">Output Directory (where should results file go on
PC?):</th>
</tr>
<tr class="header">
<th colspan="4">Tabulate by Precinct:</th>
<th colspan="2">Y N (Circle One)</th>
<th>Generate CDF JSON:</th>
<th>Y N (Circle One)</th>
</tr>
<tr class="odd">
<th colspan="8">Configuration File Name:</th>
</tr>
<tr class="header">
<th colspan="5">Date of Test:</th>
<th colspan="3">Passed?: YES NO</th>
</tr>
<tr class="odd">
<th colspan="8">Name of tester(s)</th>
</tr>
<tr class="header">
<th colspan="5">Name 1</th>
<th colspan="3">Name 2</th>
</tr>
<tr class="odd">
<th colspan="8"></th>
</tr>
<tr class="header">
<th colspan="8">‡Disclaimer: The Hare Quota tabulation option in the
RCTab software has not been thoroughly tested in a controlled testing
lab environment. Do not attempt to implement this option without first
testing in a non-operational environment. Please contact the Ranked
Choice Voting Resource Center for additional information.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## RCTab Test Structure Form Example:

<table>
<colgroup>
<col style="width: 9%" />
<col style="width: 17%" />
<col style="width: 7%" />
<col style="width: 1%" />
<col style="width: 13%" />
<col style="width: 5%" />
<col style="width: 14%" />
<col style="width: 31%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="8"><strong>RCTab Test Structure Form</strong></th>
</tr>
<tr class="odd">
<th colspan="8" rowspan="3">This form is the basic template used to
define every test setting for each test set used to test the RCTab. This
form is filled out with the settings for the 2015 Portland Mayor test.
Testers can compare the information in this form to the information in
the configuration file loaded from the relevant folder in test_data to
ensure that all settings are correct. Each setting selected in this form
is a setting tested by this test. RCTab has not yet created a Test
Structure Form for every RCTab test, but will set up a form for all
RCTab regression tests based on this form. Questions about this form can
be made to the Ranked Choice Voting Resource Center at
info@rcvresources.org or by calling 1-833-868-3728.</th>
</tr>
<tr class="header">
</tr>
<tr class="odd">
</tr>
<tr class="header">
<th colspan="8"></th>
</tr>
<tr class="odd">
<th colspan="8"><strong>Contest Info</strong></th>
</tr>
<tr class="header">
<th colspan="4">Contest Name*:</th>
<th colspan="2">Portland 2015 Mayoral Race</th>
<th>Contest Date:</th>
<th>2015-11-03</th>
</tr>
<tr class="odd">
<th colspan="4">Contest Jurisdiction:</th>
<th colspan="2">Portland, ME</th>
<th>Contest Office:</th>
<th>Portland, ME</th>
</tr>
<tr class="header">
<th colspan="4">Rules Description:</th>
<th colspan="4"></th>
</tr>
<tr class="odd">
<th colspan="8"><strong>CVR Files</strong></th>
</tr>
<tr class="header">
<th colspan="2">Provider*:</th>
<th colspan="3">ES&amp;S</th>
<th colspan="3"><em>Enter Vendor (ES&amp;S, etc.)</em></th>
</tr>
<tr class="odd">
<th colspan="2">File Path*:</th>
<th colspan="3">2015_portland_mayor_cvr.xlsx</th>
<th colspan="3"><em>Location of the cvr export file.</em></th>
</tr>
<tr class="header">
<th colspan="2">First Vote Column Index*:</th>
<th colspan="3">4</th>
<th colspan="3"><em>Enter the Column where the First Vote is in the CVR
export file.</em></th>
</tr>
<tr class="odd">
<th colspan="2">First Vote Row Index*:</th>
<th colspan="3">2</th>
<th colspan="3"><em>Enter the Row where the First Vote is in the CVR
export file.</em></th>
</tr>
<tr class="header">
<th colspan="2">ID Column Index:</th>
<th colspan="3">Leave blank</th>
<th colspan="3"><em>Enter the ID Column (if being used)</em></th>
</tr>
<tr class="odd">
<th colspan="2">Precinct Column Index:</th>
<th colspan="3">2</th>
<th colspan="3"><em>Enter the Precinct Column in the CVR export
file.</em></th>
</tr>
<tr class="header">
<th colspan="2">Overvote Label</th>
<th colspan="3">overvote</th>
<th colspan="3"><em>ES&amp;S will default to “overvote”</em></th>
</tr>
<tr class="odd">
<th colspan="2">Undervote Label</th>
<th colspan="3">undervote</th>
<th colspan="3"><em>ES&amp;S will default to “undervote”</em></th>
</tr>
<tr class="header">
<th colspan="2">Undeclared Write-In Label</th>
<th colspan="3">UWI</th>
<th colspan="3"><em>User will define. Must match case and spelling with
CVR</em></th>
</tr>
<tr class="odd">
<th colspan="2">Treat Blank as Undeclared WI</th>
<th colspan="3">FALSE</th>
<th colspan="3"><em>Circle One</em></th>
</tr>
<tr class="header">
<th colspan="8"><strong>Candidates</strong></th>
</tr>
<tr class="odd">
<th>Name*:</th>
<th colspan="2">See below for a list of candidates.</th>
<th colspan="5"><em>Obtain a list of candidates from the EMS System
(Attach the list of candidates to this form)</em></th>
</tr>
<tr class="header">
<th>Code:</th>
<th colspan="2"></th>
<th colspan="5"><em>Enter the Code for each candidate (if being used)
example: DDE</em></th>
</tr>
<tr class="odd">
<th>Excluded:</th>
<th colspan="2"></th>
<th colspan="5"><em>Check box if a candidate is not being counted in
this tabulation</em></th>
</tr>
<tr class="header">
<th colspan="8"><strong>Winning Rules</strong></th>
</tr>
<tr class="odd">
<th colspan="4">Winner Election Mode*</th>
<th colspan="2">singleWinnerMajority</th>
<th colspan="2" rowspan="6">Any user jurisdiction guidelines which
identify the specific rules for ranked choice voting elections should be
attached to this form for easy reference. Please see the RCTab User
Guide for more information about the available selections that can be
made.</th>
</tr>
<tr class="header">
<th colspan="4">Maximum # of Ranked Candidates*</th>
<th colspan="2">15</th>
</tr>
<tr class="odd">
<th colspan="4">Minimum Vote Threshold</th>
<th colspan="2">0</th>
</tr>
<tr class="header">
<th colspan="4">Use Batch Elimination</th>
<th colspan="2"><strong>Y</strong> N (Circle One)</th>
</tr>
<tr class="odd">
<th colspan="4">Continue until Two Candidates Remain</th>
<th colspan="2">Y <strong>N</strong> (Circle One)</th>
</tr>
<tr class="header">
<th colspan="4">Tiebreak Mode*</th>
<th colspan="2">useCandidateOrder</th>
</tr>
<tr class="odd">
<th colspan="4">Random Seed*</th>
<th colspan="2">N/A</th>
<th colspan="2" rowspan="8">Any user jurisdiction guidelines which
identify the specific rules for ranked choice voting elections should be
attached to this form for easy reference. Please see the RCTab User
Guide for more information about the available selections that can be
made.</th>
</tr>
<tr class="header">
<th colspan="4">Number of Winners*</th>
<th colspan="2">1</th>
</tr>
<tr class="odd">
<th colspan="4">Percentage Threshold*</th>
<th colspan="2">N/A</th>
</tr>
<tr class="header">
<th colspan="4">Threshold Calculation Method*</th>
<th colspan="2">N/A</th>
</tr>
<tr class="odd">
<th colspan="4">Most Common Threshold</th>
<th colspan="2">N/A</th>
</tr>
<tr class="header">
<th colspan="4">HB Quota</th>
<th colspan="2">N/A</th>
</tr>
<tr class="odd">
<th colspan="4">Hare Quota‡</th>
<th colspan="2">N/A</th>
</tr>
<tr class="header">
<th colspan="4">Decimal Places for Vote Arith (MW ONLY)*</th>
<th colspan="2">N/A</th>
</tr>
<tr class="odd">
<th colspan="8"><strong>Voter Error Rules</strong></th>
</tr>
<tr class="header">
<th colspan="4">Overvote Rule*</th>
<th colspan="2">Exhaust Immediately</th>
<th colspan="2">Choose one of the three available options.</th>
</tr>
<tr class="odd">
<th colspan="4">Skip to next rank</th>
<th colspan="2">-</th>
<th colspan="2"></th>
</tr>
<tr class="header">
<th colspan="4">Exhaust Immediately</th>
<th colspan="2">X</th>
<th colspan="2"></th>
</tr>
<tr class="odd">
<th colspan="4">Exhaust if mult. cont.</th>
<th colspan="2">-</th>
<th colspan="2"></th>
</tr>
<tr class="header">
<th colspan="4">Consecutive Skip Ranks Allowed</th>
<th colspan="2">1</th>
<th colspan="2">Enter the number of consecutive skipped rankings or
check Unlimited if permitted.</th>
</tr>
<tr class="odd">
<th colspan="4">Exhaust on multi ranks for same candidate</th>
<th colspan="2">Y <strong>N</strong> (Circle One)</th>
<th colspan="2">Check if multiple ranks for the same candidate are not
permitted.</th>
</tr>
<tr class="header">
<th colspan="8"><strong>Output</strong></th>
</tr>
<tr class="odd">
<th colspan="8">Output Directory (where should results file go on PC?):
output</th>
</tr>
<tr class="header">
<th colspan="4">Tabulate by Precinct:</th>
<th colspan="2">Y <strong>N</strong> (Circle One)</th>
<th>Generate CDF JSON:</th>
<th>Y <strong>N</strong> (Circle One)</th>
</tr>
<tr class="odd">
<th colspan="8">Configuration File Name:
2015_portland_mayor_config.json</th>
</tr>
<tr class="header">
<th colspan="5">Date of Test:</th>
<th colspan="3">Passed?: YES NO</th>
</tr>
<tr class="odd">
<th colspan="8">Name of tester(s)</th>
</tr>
<tr class="header">
<th colspan="5">Name 1</th>
<th colspan="3">Name 2</th>
</tr>
<tr class="odd">
<th colspan="8" rowspan="2">‡Disclaimer: The Hare Quota tabulation
option in the RCTab software has not been thoroughly tested in a
controlled testing lab environment. Do not attempt to implement this
option without first testing in a non-operational environment. Please
contact the Ranked Choice Voting Resource Center for additional
information.</th>
</tr>
<tr class="header">
</tr>
<tr class="odd">
<th>Candidates</th>
<th>Brennan, Michael F.</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="header">
<th></th>
<th>Bragdon, Charles E.</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="odd">
<th></th>
<th>Bryant, Peter G.</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="header">
<th></th>
<th>Carmona, Ralph C.</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="odd">
<th></th>
<th>Dodge, Richard A.</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="header">
<th></th>
<th>Duson, Jill C.</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="odd">
<th></th>
<th>Eder, John M.</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="header">
<th></th>
<th>Haadoow, Hamza A.</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="odd">
<th></th>
<th>Lapchick, Jodie L.</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="header">
<th></th>
<th>Marshall, David A.</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="odd">
<th></th>
<th>Mavodones, Nicholas M. Jr.</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="header">
<th></th>
<th>Miller, Markos S.</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="odd">
<th></th>
<th>Rathband, Jed</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="header">
<th></th>
<th>Strimling, Ethan K.</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
<tr class="odd">
<th></th>
<th>Vail, Christopher L.</th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
<th></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

## List of Tabulator Tests

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
same candidate" function, if function is turned off - RCTab will ignore
duplicate candidate rankings. Test the ability of RCTab to export CVR
data in the CDF.</th>
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
<th>Test the functionality of the multi-winner allow only one winner per
round logic.</th>
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
<col style="width: 16%" />
<col style="width: 37%" />
<col style="width: 45%" />
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
<th>When the cryptographic signature of the Hart CVR is incorrect,
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

<table>
<colgroup>
<col style="width: 15%" />
<col style="width: 11%" />
<col style="width: 48%" />
<col style="width: 25%" />
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
<th>11/09/2023</th>
<th>1.4.2</th>
<th>Updated with v1.3.2 Security Tests</th>
<th>Mathew Ruberg</th>
</tr>
<tr class="odd">
<th>4/28/2023</th>
<th>1.4.1</th>
<th>Updated to reflect RCTab version 1.3.1</th>
<th>Kelly Sechrist</th>
</tr>
<tr class="header">
<th>4/26/2023</th>
<th>1.4.0</th>
<th>Updated to reflect RCTab version 1.3.0</th>
<th>Chris Hughes</th>
</tr>
<tr class="odd">
<th>01/05/2022</th>
<th>1.3.0</th>
<th>Updated document to reflect RCTab and remove NY</th>
<th>Rene Rojas</th>
</tr>
<tr class="header">
<th>05/14/2021</th>
<th>1.2.0</th>
<th>Added test conditions. Updated Test data link to include file with
all results. Added test structure information/form.</th>
<th>Kelly Sechrist</th>
</tr>
<tr class="odd">
<th>05/10/2021</th>
<th>1.1.0</th>
<th>Update to provide more details on past test behaviors and future
plans for improving test tracking and control.</th>
<th>Chris Hughes</th>
</tr>
<tr class="header">
<th>05/02/2021</th>
<th>1.0.0</th>
<th>System Test and Verification</th>
<th>Chris Hughes</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

# 

