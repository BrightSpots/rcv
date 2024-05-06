# RCTab v1.3.2 Section 15 - System Change Notes v.1.3.1 

> **RCTab v1.3.2 Section 15 - System Change Notes v.1.3.1** document is
> solely for use in the State of California. This document can be
> expanded or updated as is necessary or required. Where relevant, this
> document refers to specific sections and requirements of the
> California Voting System Standards. Any recommendations listed in this
> document should not supersede user jurisdiction procedures or other
> controlling governance entities.
>
> Any recommendations listed in this document should not supersede user
> jurisdiction procedures or other controlling governance entities.

## 9.12 System Change Notes

> Manufacturers submitting modifications for a system that has been
> tested previously and received national certification shall submit
> system change notes. These will be used by the S-ATA to assist in
> developing and executing the test plan for the modified system. The
> system change notes shall include the following information:

1.  Summary description of the nature and scope of the changes, and
    reasons for each change

2.  A listing of the specific changes made, citing the specific system
    configuration items changed and providing detailed references to the
    documentation sections changed

3.  The specific sections of the documentation that are changed (or
    completely revised documents, if more suitable to address a large
    number of changes)

4.  Documentation of the test plan and procedures executed by the
    manufacturer for testing the individual changes and the system as a
    whole, and records of test results

## RCTab Version 1.3.2

RCTab version 1.3.1 was submitted with Hart InterCivic’s Verity Voting
3.2 to SLI for initial testing to California Voting System Standards.
After an initial round of testing we received SLI’s report. Some of the
report results identified recommendations for RCTab mostly related to
access control and programmatically verifying RCTab input and output.
Ranked Choice Voting Resource Center (RCVRC) is submitting a new
version, v1.3.2, to address each of the report results.

**New Features**

-   Two Windows OS accounts. One Administrator level Windows account for
    installation and initial configuration. A separate ‘RCTab’ Windows
    standard account for running the tabulation with only necessary
    permissions and **will not have access to make administrative
    changes to the machine** that could be security issues

-   Read-Only RCTab output files, each with a corresponding read-only
    .hash file that contains the hash of file contents to ensure they
    haven’t been edited

-   Automatic, programmatic verification of the cryptographic signature
    of all input CVRs. This verification step ensures both the integrity
    (CVR contents have not been edited) and provenance (CVRs came from
    the Hart voting system) of the Hart CVRs. RCTab will throw a halting
    error and tabulation will not begin if cryptographic validation of
    Hart’s CVR signature is not successful.

**Hardware Setup Improvements**

-   Explicit TDP directions for a secure, hardened OS install

-   Password secured BIOS

**TDP Updates**

-   **RCTab v1.3.2 Section 02 - Software Design and Specifications**

    -   Includes .hash file descriptions in **Reporting Results**
        header, **RCTab Logging Stream 2: Contest-Specific "Audit"
        Logging** header, ResultsWriter Java class

    -   Removes paragraph in **RCTab Logging - Stream 2:
        Contest-Specific "Audit" Logging** header about config
        validation missing from audit log. That was fixed in 1.3.1

    -   9.5.3.c.3 updated with link to Section 14 - Tabulator Trusted
        Build Instructions

    -   3rd Party Modules section updated with new version numbers and
        added BouncyCastle FIPS API module

    -   Include new 1.3.2 Java classes: AuditableFile, SecurityConfig,
        SecuritySignatureValidation, SecurityXmlParsers, SecurityTests

-   **RCTab v1.3.2 Section 07 - System Security Specification
    Requirements**

    -   Add RCTab Windows standard user account, read-only outputs,
        .hash files for RCTab output and Hart CVR signature verification
        throughout CVSS security & access control requirements

        -   9.6.1.a, 9.6.1.b, 9.6.1.c, 9.6.1.d, 9.6.1.e, 9.6.1.1,
            9.6.1.1.a, 9.6.2

    -   9.6.1.b updated to include Windows OS Account and BIOS Password
        access control mechanisms

    -   9.6.1.2 updated to include full description of two tier Windows
        OS accounts for access control

    -   9.6.1.1.e updated to include explicit overview of Operating
        System protection abilities

    -   9.6.3.f describes in detail the signature verification
        interaction with Hart Verity

    -   9.6.7 Cryptography header updated with explicit cryptographic
        signature verification details

-   **RCTab v1.3.2 Section 07I - Design and Interface Specification**

    -   Includes all security enhancements for 1.3.2 and describes the
        threat that they protect against as well as the threats that
        still exist

    -   Added explicit system security objectives and known
        vulnerabilities

-   **RCTab v1.3.2 Section 07J - Security Architecture**

    -   Include 1.3.2 updates in Access Control and Integrity sections

-   **RCTab v1.3.2 Section 07L - Security Threat Analysis**

    -   Include all 1.3.2 updates and describe the threats that they
        protect against

-   **RCTab v1.3.2 Section 07M - Security Testing and Vulnerability
    Analysis**

    -   Include all 1.3.2 updates to describe how initial testing report
        results have all been addressed

-   **RCTab v1.3.2 Section 08 - System Operations Procedures**

    -   Include automatic signature verification of Hart signed CVRs,
        .hash output files and read-only output.

    -   Output Directory instructions detail explicit steps to ensure
        read-only permissions are set

    -   Include directions to configure Verity Count to sign CVR exports

    -   Remove manual hash steps in ‘Generating Results Files’ header

-   **RCTab v1.3.2 Section 09 - System Maintenance Manual**

    -   Includes read-only output and .hash file verification in 9.9.1.f

-   **RCTab v1.3.2 Section 11 - L&A Testing**

    -   Update hash verification of output with .hash files

-   **RCTab v1.3.2 Section 12 - Configuration Management Plan**

    -   Update RCTab Workstation model, hash value of v1.3.2, required
        software

-   **RCTab v1.3.2 Section 13 - Quality Assurance Plan**

    -   Adds security tests for v1.3.2

-   **RCTab v1.3.2 Section 14 - Tabulator Trusted Build Instructions**

    -   Added explicit instructions for setting JAVA\_HOME variable

    -   Updated Comparing Two Builds section to explain more clearly how
        we get a single hash, remove ‘Individual Files’ section

-   **RCTab v1.3.2 Section 15 - System Change Notes**

    -   Include code updates, TDP updates

-   **RCTab v1.3.2 Section 16 - System Hardening Procedures - Windows
    OS**

    -   Included BIOS password, Windows OS Installation

    -   Make clear the distinction between online prep and offline
        updates

    -   Explicit steps for enabling Bitlocker device encryption

    -   Edit ‘Operating System Hardening’ header to disable screensaver
        and correctly disable Remote Desktop

    -   Remove additional optional antivirus, Windows Update

    -   Replace Excel with LibreOffice

    -   Remove manual driver installation

-   **RCTab v1.3.2 Section 17 - System Test and Verification
    Specification**

    -   Adds security tests for v1.3.2

-   **RCTab v1.3.2 Section 18 - User Guide**

    -   Programmatic CVR signature verification in Hart CVR Files
        Options header and Prepping CVRs For Use header

    -   Instructions for Read-Only output in Generating Results Files -
        Output Tab header

    -   Running A Tabulation header output files updated to include
        .hash files

    -   Remove CVR .csv from list of output files as it doesn’t apply to
        Hart

-   **RCTab v1.3.2 Section 22 - Installation Instructions for Windows
    OS**

    -   Include instructions for creating ‘RCTab’ Windows Standard User

    -   Explicit instructions for RCTab Windows Standard user vs.
        Windows Administrator user

    -   Include directions for enforcing read-only permissions on output
        folder

    -   Include directions for creating desktop shortcut

-   **RCTab v.1.3.1 Section 23 - Trusted Build & Output Hash
    Verification - Windows OS**

    -   Use .hash files for output summary file verification. Use
        different example path that doesn’t use user folders

-   **RCTab v1.3.2 Section 24 - Tabulator Command Line Instructions**

    -   Use different example path outside of user paths

-   **RCTab v1.3.2 Section 26 - RCTab CVR Files**

    -   Include .sig.xml file description for required Hart CVR archive
        cryptographic signature

-   **RCTab v1.3.2 Section 28 - Post-Election Audit & Clearing RCTab
    from System**

    -   Include comparing the text of output file .hash files to the
        hash text in the audit log as another auditable check

-   **RCTab v1.3.2 Section 29 - RCTab Operator Log Messages**

    -   Added additional v1.3.2 Severe messages

## RCTab Version v.1.3.1

> **Bug fixes:**

-   Fixed XML parsing failing when running built version
    ([<u>\#625</u>](https://github.com/BrightSpots/rcv/issues/625))

> **Backend updates:**

-   Releases for all platforms are now automatically built by GitHub
    when published
    ([<u>\#282</u>](https://github.com/BrightSpots/rcv/issues/282))

## RCTab Version v.1.3.0

> **New features:**

-   Added support for multi-file Dominion format
    ([\#569](https://github.com/BrightSpots/rcv/pull/569))

-   Allows batch elimination and "continue until two candidates remain"
    to be enabled in multi-pass IRV mode
    ([\#611](https://github.com/BrightSpots/rcv/pull/611))

-   Allows users to specify multiple CVR files at once in the GUI
    ([\#617](https://github.com/BrightSpots/rcv/pull/617))

-   Adds validation highlighting to the GUI when clicking the "add"
    buttons for candidates and CVRs
    ([\#618](https://github.com/BrightSpots/rcv/pull/618))

-   Changed audit logs to include validation outcome
    ([\#616](https://github.com/BrightSpots/rcv/pull/616))

### 

> **Bug fixes:**

-   Fixed Hare Quota
    ([\#562](https://github.com/BrightSpots/rcv/pull/562))

-   Fixed build for M1 Macbooks
    ([\#586](https://github.com/BrightSpots/rcv/pull/586))

-   Fixed crashes when % was in file paths
    ([\#601](https://github.com/BrightSpots/rcv/pull/601))

-   Fixed inaccurate overvoteRule error message
    ([\#609](https://github.com/BrightSpots/rcv/pull/609))

-   Fixed bug in logic for exhaustIfMultipleContinuing overvote rule
    ([\#610](https://github.com/BrightSpots/rcv/pull/610))

-   Fixed bug where treatBlankAsUndeclaredWriteIn validation failure for
    certain providers wouldn't actually fail validation
    ([\#618](https://github.com/BrightSpots/rcv/pull/618))

### 

> **Other improvements**

-   Rebranded "Universal RCV Tabulator" as "RCTab"
    ([\#603](https://github.com/BrightSpots/rcv/pull/603))

-   Updated license from AGPL to MPL 2.0
    ([\#604](https://github.com/BrightSpots/rcv/pull/604))

-   Exits gracefully if all declared candidates fall beneath minimum
    vote threshold
    ([\#608](https://github.com/BrightSpots/rcv/pull/608))

-   Moved validation of provided overvote delimiter and overvote label
    into performBasicCvrSourceValidation so user is alerted when
    clicking the "add" button in the CVR tab
    ([\#618](https://github.com/BrightSpots/rcv/pull/618))

-   Updated documentation and help text
    ([\#547](https://github.com/BrightSpots/rcv/pull/547),
    [\#614](https://github.com/BrightSpots/rcv/pull/614),
    [\#617](https://github.com/BrightSpots/rcv/pull/617))

> **Backend updates:**

-   Enabled CI, which runs tests, Checkstyle, and Spotbugs
    ([\#576](https://github.com/BrightSpots/rcv/pull/576))

-   Addressed all outstanding Checkstyle and Spotbugs warnings
    ([\#587](https://github.com/BrightSpots/rcv/pull/587))

-   Internal clean-up to conform to VVSG coding requirements
    ([\#600](https://github.com/BrightSpots/rcv/pull/600),
    [\#602](https://github.com/BrightSpots/rcv/pull/602),
    [\#606](https://github.com/BrightSpots/rcv/pull/606))

-   Changed ContestConfig.validate() and associated methods to return a
    set of validation errors instead of an isValid boolean
    ([\#618](https://github.com/BrightSpots/rcv/pull/618))

-   checkstyle-suppressions.xml location is now handled in build.gradle
    to avoid needing to manually modify google\_checks.xml in the future
    ([\#544](https://github.com/BrightSpots/rcv/pull/544))

-   Updated dependencies to latest versions:

    -   JDK 17.0.2

    -   JavaFX 18

    -   Gradle 7.5.1

    -   Checkstyle google\_checks.xml 10.3.2

    -   Checkstyle plugin 10.3.2

    -   spotbugs 4.7.1

    -   spotbugs-gradle-plugin 5.0.9

    -   org.openjfx.javafxplugin 0.0.11

    -   org.beryx.jlink 2.25.0

    -   com.fasterxml.jackson.core:jackson-\* 2.13.3

    -   org.junit.jupiter.junit-jupiter-\* 5.9.0

    -   org.apache.commons:commons-csv 1.9.0

    -   org.apache.poi:poi-ooxml 5.2.2

## Universal RCV Tabulator (RCTab) Version v.1.2.0 

> **Certification:**
>
> Based on the testing performed by Pro V&V, a certified Voting System
> Testing Laboratory, and the results obtained, the modified
> RCV-Tabulator v.1.2.0 solution identified in this test report meets
> the requirements set forth by the VVSG 1.0.
>
> **New features:**
>
> • Added support for new manufacturer formats:
>
> o Clear Ballot (<u>\#400</u>)
>
> o Dominion (<u>\#119</u>, <u>\#438</u>, <u>\#533</u>)
>
> o Hart (<u>\#401</u>, <u>\#457</u>, <u>\#460</u>)
>
> o Unisyn (<u>\#402</u>)
>
> • Redesigned GUI to be more user-friendly (<u>\#461</u>, <u>\#123</u>,
> <u>\#128</u>, <u>\#152</u>) (see "GUI redesign" section below for more
> details)
>
> • Added support for "Overvote Delimiter" field (<u>\#482</u>)
>
> • "Minimum Vote Threshold" field is now optional (<u>\#483</u>)
>
> • "Undeclared Write-In Label", "Overvote Label", "Undervote Label",
> and "Treat Blank as Undeclared Write-in" fields are now defined on a
> per-CVR level (<u>\#508</u>)
>
> **GUI redesign:**
>
> • Added hint panels for each tab (<u>\#499</u>)
>
> • Added help menu option for the config documentation (<u>\#497</u>,
> <u>\#528</u>)
>
> • Changed "Continue until Two Candidates Remain" to a boolean config
> setting (<u>\#481</u>)
>
> • Combo text box / check box input for "How Many Consecutive Skipped
> Ranks Are Allowed" and "Maximum Number of Candidates That Can Be
> Ranked" (<u>\#498</u>)
>
> • Disabled "Decimal Places for Vote Arithmetic" in non-multi-seat
> modes (<u>\#500</u>)
>
> • Redesigned nonIntegerWinningThreshold and hareQuota as a three-way
> radio button and added new validation rules (<u>\#501</u>)
>
> • Disabled editing existing candidates and CVR sources after adding to
> prevent confusing UX (<u>\#502</u>)
>
> • Added support for multiple contests via implementation of "Contest
> ID" field (<u>\#456</u>, <u>\#472</u>, <u>\#478</u>)
>
> **Additional GUI changes:**
>
> • Split Output tab into new Contest Info and Output tabs

• Redesigned GUI CVR Files tab, adding Clear button, and changing Add
button so it only

> enter multiple sources that share fields clears the file path to make
> it easier to manually • Improved visual presentation of Candidate tab;
> added Clear button and adds checkBoxCandidateExcluded when adding a
> candidate
>
> • Reorganized presentation of rules in "Winning Rules" and "Voter
> Error Rules" tabs
>
> • Winner Election Mode and Tiebreak Mode now start undefined with all
> relevant fields disabled; choosing specific modes enables applicable
> fields
>
> • Changed Winner Election Modes and Tiebreak Modes to be more
> user-friendly, including necessary migration logic to update older
> config files
>
> • Expanded footprint of GUI window to 1200x1000
>
> • Implemented bordered boxes
>
> • Converted overvoteRule from a ChoiceBox to an array of RadioButtons;
> changed overvoteRule string display in config files and adds migration
> logic
>
> • Disabled decimalPlacesForVoteArithmetic and
> nonIntegerWinningThreshold except when winnerElectionMode is
> "Multi-winner allow only one winner per round" or "Multi winner allow
> multiple winners per round" (fixes <u>\#500</u>)
>
> • Added suggested values for overvoteLabel, undervoteLabel, and ES&S
> column and row indices
>
> • Replaced checkBoxNonIntegerWinningThreshold and checkBoxHareQuota
> with a radio button array, and added new validation rules for those
> settings (fixing <u>\#501</u>)
>
> • GUI now disables numberOfWinners field and sets it to 1 only
>
> when winnerElectionMode is "Single-winner majority determines winner"
>
> • GUI now disables numberOfWinners field and sets it to 0 when
> winnerElectionMode is "Bottoms-up using percentage threshold"
>
> • Fixed bugs in validation error messages when numberOfWinners is 0
> **Bug fixes:**
>
> • Fixes CDF JSON reading and writing (<u>\#505</u>)
>
> • Fixed being unable to tabulate multiple CDF sources (<u>\#536</u>)
>
> • Fixed user and computer name logging (<u>\#521</u>)
>
> • Fixed config referencing nonexistent CDF source leading to uncaught
> exception (<u>\#347</u>) • Fixed incorrect overvote label in CDF
> leading to NPE (<u>\#453</u>)
>
> • Fixed config file with bad provider value failing with NPE
> (<u>\#531</u>)
>
> **Other improvements:**
>
> • Removed "Convert Dominion to Generic Format..." functionality since
> direct Dominion tabulation is now possible (<u>\#476</u>)
>
> • Registers explicit overvote as valid candidate / contest selection
> in CDF output when needed (<u>\#451</u>)
>
> • Handles bad path to CDF CVR source gracefully (<u>\#452</u>)
>
> • Handles empty rows at end of CVR (<u>\#455</u>)
>
> • Made sure all providers work with the CLI (<u>\#471</u>)
>
> • Raises error if we encounter an unrecognized candidate while loading
> Dominion CVRs during direct tabulation (<u>\#473</u>)
>
> • Reports error if config specifies any column indexes for a CDF
> source (<u>\#276</u>) **Backend updates:**
>
> • Created separate MigrationHelper class (<u>\#507</u>)
>
> • Addressed warnings during Gradle build (<u>\#280</u>)
>
> • Upgrading to a more recent version of Gradle no longer causes test
> failures (<u>\#283</u>) • Addressed all relevant Checkstyle warnings
> and disabled all invalid ones (<u>\#490</u>, <u>\#489</u>) • Enum
> parameters now use camel case for backend and user-friendly strings
> for frontend (<u>\#494</u>)
>
> • Fixed noinspection unchecked for excluded CheckBox in
> GuiConfigController (<u>\#304</u>) • Now
>
> use UnrecognizedCandidatesException in ClearBallotCvrReader and
> HartCvrReader (<u>\#4</u> <u>91</u>)
>
> • Updated dependencies to latest version:
>
> o JDK 14.0.1
>
> o JavaFX 14.0.1
>
> o Gradle 6.5.1
>
> o Checkstyle google\_checks.xml 8.36.2
>
> o Checkstyle plugin 8.36.2
>
> o org.openjfx.javafxplugin 0.0.9
>
> o org.beryx.jlink 2.20.0
>
> o com.fasterxml.jackson.core:jackson-\* 2.11.1
>
> o org.junit.jupiter.junit-jupiter-\* 5.6.2

## Universal RCV Tabulator (RCTab) Version v.1.1.0 

> **Certification:**
>
> Based on the testing performed by Pro V&V, a certified Voting System
> Testing Laboratory, and the results obtained, the Universal RCV
> Tabulator v1.1.0 solution is believed to meet the applicable
> requirements set forth by the EAC-approved VVSG 1.0.
>
> **New features:**
>
> • Added support for converting Dominion JSON CVRs to generic .csv
> format (including precinct portions) (<u>\#404</u>, <u>\#406</u>,
> <u>\#407</u>, <u>\#408</u>, <u>\#415</u>, <u>\#439</u>)
>
> • Added multiSeatBottomsUpPercentageThreshold option (<u>\#403</u>)
>
> • Added CLI option to convert Dominion CVR to generic .csv
> (<u>\#408</u>)
>
> • New GUI menu and conversion options (can now convert to CDF and
> convert Dominion to generic via the GUI) (<u>\#408</u>, <u>\#421</u>)
>
> • Added Dominion Alaska CVR to sample\_input folder
>
> **Bug fixes:**
>
> • Batch elimination now works properly with
>
> singleSeatContinueUntilTwoCandidatesRemain (<u>\#396</u>)
>
> • In a multi-seat contest, if someone wins in the first round, we now
> automatically eliminate undeclared write-ins before we eliminate any
> other candidates; previously, we treated UWIs like a normal candidate,
> which meant we potentially eliminated other candidates with lower
> tallies first (<u>\#397</u>)
>
> • If UWI exceeds the winning threshold in the initial count, we no
> longer mistakenly elect this candidate (<u>\#398</u>)
>
> **Backend updates:**
>
> • Updated dependencies to latest version: JDK, JavaFX, Checkstyle
> google\_checks.xml, Checkstyle plugin, org.openjfx.javafxplugin,
> org.beryx.jlink,
>
> org.apache.commons:commons-csv, org.apache.poi:poi-ooxml,
>
> com.fasterxml.jackson.core:jackson-\*
>
> • Added special code to test configs to obviate the need to update the
> version with each increment (<u>\#426</u>)
>
> • Updated tests and improved test coverage
>
> • Copyright update (<u>\#414</u>)
>
> • Code cleanup

## Universal RCV Tabulator (RCTab) Version v.1.0.1 

> Based on the testing performed by Pro V&V, a certified Voting System
> Testing Laboratory, and the results obtained, the Universal RCV
> Tabulator solution meets the requirements set forth by the
> EAC-approved VVSG 1.0 to be used with the ES&S EVS 5.0.0.0 through
> 6.0.4.0 software. Changelog:
>
> • Added Checkstyle plugin to Gradle and set it up for Google format
>
> • Minor refactoring to address Checkstyle issues

**Document Revision History**

<table>
<colgroup>
<col style="width: 15%" />
<col style="width: 11%" />
<col style="width: 48%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Date</strong></th>
<th><strong>Version</strong></th>
<th><strong>Description</strong></th>
<th><strong>Author</strong></th>
</tr>
<tr class="odd">
<th>11/02/2023</th>
<th>1.3.1</th>
<th>Include code updates and TDP updates for RCTab v 1.3.2</th>
<th>Mathew Ruberg</th>
</tr>
<tr class="header">
<th>04/27/2023</th>
<th>1.2.1</th>
<th>Updated to reflect RCTab v. 1.3.1</th>
<th>Chris Hughes</th>
</tr>
<tr class="odd">
<th>04/18/2023</th>
<th>1.2.0</th>
<th>Changed to reflect CA Voting System Standards</th>
<th>Ryan Kirby</th>
</tr>
<tr class="header">
<th>04/05/2023</th>
<th><blockquote>
<p>1.1.0</p>
</blockquote></th>
<th><blockquote>
<p>Updated to Reflect RCTab v.1.3.0</p>
</blockquote></th>
<th><blockquote>
<p>Rene Rojas</p>
</blockquote></th>
</tr>
<tr class="odd">
<th>09/07/2021</th>
<th><blockquote>
<p>1.0.1</p>
</blockquote></th>
<th><blockquote>
<p>Revised to non state specific.</p>
</blockquote></th>
<th><blockquote>
<p>Kelly Sechrist</p>
</blockquote></th>
</tr>
<tr class="header">
<th>04/22/2021</th>
<th><blockquote>
<p>1.0.0</p>
</blockquote></th>
<th><blockquote>
<p>System Change Notes</p>
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

