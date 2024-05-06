# RCTab v1.3.2 Section 11 - L&A Testing v.1.3.2

> **RCTab v1.3.2 Section 11 - L&A Testing v.1.3.2** document is solely
> for use in the State of California. This document can be expanded or
> updated as is necessary or required. Where relevant, this document
> refers to specific sections and requirements of the California Voting
> System Standards. Any recommendations listed in this document should
> not supersede user jurisdiction procedures or other controlling
> governance entities.

The Logic & Accuracy Test is designed to verify that RCTab is correctly
configured and operating properly. Logic & accuracy tests should always
be conducted on RCTab prior to its use in an election as part of the
user jurisdiction’s full logic & accuracy tests for each election.
Post-election logic & accuracy testing should also be performed where
possible and in keeping with the established policies and procedures of
the user jurisdiction.

## Required Materials

In addition to the following, you must know the correct version of your
operating system and RCTab. The manufacturer can provide assistance in
determining the correct version. Users can also find that information in
**RCTab v1.3.2 Section 22 - Installation Instructions for Windows OS
v.1.3.0** documentation. The following components of a voting system are
required to complete the acceptance testing procedures:

1.  One Computer with RCTab installed. Installation instructions can be
    found **RCTab v1.3.2 Section 22 - Installation Instructions for
    Windows OS v.1.3.0** document.

2.  A general use and directly connected printer (this includes
    necessary printer cable and power supply).

3.  Recommended battery backup if the user jurisdiction facility does
    not have generator capabilities in the event of a power failure.

4.  At minimum, one flash drive containing a CVR from your voting system
    with ranked choice voting results.

5.  One additional storage device that can be used for saving summary
    files.

The RCTab computer system, consisting of items 1 through 3 above, should
have been set up and tested before beginning this test. IT staff may do
this part of the setup if properly supervised by election staff who have
been trained in accordance with procedures laid out in the personnel
documentation.

## Verify RCTab Hardware

Verify that all hardware components of the RCTab computer are turned on
and functioning properly. This includes checks of the printer(s), flash
drives, USB port, etc.

## Verify Correct Operating System and RCTab Software

Turn on the computer with RCTab installed. As the computer boots up,
verify that the correct versions of the operating system and the RCTab
software are installed. Compute the hash codes for the RCTab voting
system software and compare them with the hash value provided with the
trusted build. See also **RCTab v1.3.2 Section 03 - System Hardware
Specification v.1.1.1** document and **RCTab v1.3.2 Section 22 -
Installation Instructions for Windows OS v.1.3.0** document for
procedures and more information.

## Logic & Accuracy Procedures

**The procedures should be done with all assigned personnel in no less
than teams of two who have received the recommended manufacturer-led
training. All manufacturer-recommended and jurisdiction-required
security procedures should be observed at all times during this
process.**

1.  **Assemble all required materials.** The next step will cover
    exporting a CVR from EMS to a flash drive if this step has not
    already been completed as part of the voting system L&A.

2.  **Export CVR File from EMS System.** This step assumes the user has
    completed logic & accuracy checks on all parts of their voting
    system, including EMS, as recommended by the system vendor. This
    step also requires the users to have a known outcome from the test
    deck prepared as part of logic and accuracy testing as required by
    the voting system vendor. Users should also confirm the known
    outcome from the test deck is, in fact, correct, and the voting
    system used to tabulate the ballots is also working properly. The
    text deck should be counted by hand BEFORE the actual L&A testing
    begins on both the user jurisdiction’s voting system and RCTab. This
    includes a headcount of the round-by-round RCV outcomes as well.

    1.  Following L&A testing procedures conducted in EMS, export the
        L&A CVR file from the jurisdiction’s EMS System and save the
        file to an empty USB flash drive. Please see the appropriate EMS
        procedures for the specific steps of this process.

    2.  Take the USB containing the exported CVR to the hardware where
        the RCTab software is installed.

3.  **Transfer the CVR File to the PC**

    1.  Create a folder on the hard drive of the RCTab PC. The
        manufacturer recommends a name associated with the specific
        election (sample: “LA - RCV Election 11-5-2019”). The
        manufacturer recommends the user specifically identify the
        folder as LA to avoid any confusion with outdated CVRs or other
        data. See point 14, page 7, for further information.

    2.  Insert USB into the hardware where the RCTab software is
        installed and open the file with Excel.

    3.  Copy the CVR export file from the USB to the new folder.

    4.  Enter the file path of the CVR file into the Election Data Form.

    5.  Remove the USB from the USB port and attach a label with the
        election name.

    6.  Place the USB in a secure storage location.

4.  **Enter Data into an Election Data Form.** Entering this information
    will make interacting with RCTab faster and lessen the chances that
    information will be entered incorrectly. All entries should be
    carefully reviewed by at least two team members assigned to work
    with the RCTab software. Using your Election Data Form, record the
    following information:

    1.  Contest Name: Name of the election – sample: LA-General
        Election, Primary Election, Municipal Election

    2.  Contest Date: Date of the actual election – sample: 11-05-2019

    3.  Contest Jurisdiction: Actual voting jurisdiction – sample:
        LA-Johnson County, City of Peoria, Town of Salem

    4.  Contest Office: RCV office being tabulated – sample: LA-City
        Council, County Commissioner

    5.  Decide if you are going to print Precinct Reports and enter Y or
        N on the Election Data Form.

    6.  Indicate on the Election Data Form if you are generating a CDF
        JSON file for export totals (Y or N). Generally, this setting
        will be left unchecked as it is not required in most
        tabulations.

    7.  Record the file path of the CVR as noted in step 3f, page 2, if
        you have not already done so.

    8.  Enter the Rules into the Election Data Form. The settings
        entered here must be in full compliance with the governing
        guidelines for ranked choice voting elections in the
        jurisdiction. (check with City, County, or State for guidance)

5.  **Open CVR File and Review**

    1.  Locate the CVR file in the election folder you created in step
        3a and right-click, scroll down, and hover over “Open with.”
        Select LibreOffice to view the file.

    2.  Locate the Column where the First Vote is and enter the Column
        in the Election Data Form.

    3.  Locate the Row where the First Vote is and enter the Row in the
        Election Data Form.

    4.  Locate the ID Column (leave blank if not used) and enter the
        Column in the Election Data Form.

    5.  Locate the Precinct Column and enter the Column in the Election
        Data Form.

    6.  Exit the CVR file.

**Before moving to step 6, your Election Data Form should be completed
with the exception of the Configuration File Name and required
signatures. Confirm and enter any missing information before
proceeding.**

1.  **Creating the Configuration File**

    1.  Start RCTab - RCTab will start ready to create a new config
        file.

    2.  Contest Info Tab

        1.  On the tab labeled “Contest Info,” enter the Contest Name,
            Contest Date, Contest Jurisdiction, and Contest Office. For
            Rules Description, enter a name that is easy to remember, as
            this can be used as the name for your final configuration
            file.

        2.  Only the Contest Name is required for this tab; however, we
            recommend all fields be completed to assist in identifying
            output files in the future.

    3.  CVR Files Tab

        1.  Select the voting system provider from the drop-down menu.

        2.  Using the “Select” button, locate the CVR file in the
            election folder and load the CVR file path.

        3.  Enter the remaining required information from the Election
            Data Form into the remaining fields and click Add.

        4.  The added information should populate in the listing just
            below the entry fields.

    4.  Candidates Tab

        1.  Click on the “Candidates” tab. Enter the first candidate
            name and press Add. Enter all candidates.

        2.  If you are using Code, you can return to each name and enter
            Code, and press enter on the keyboard.

        3.  You will not enter Exclude unless instructed to do so by the
            Election Administration Team.

    5.  Winning Rules Tab

        1.  Use the completed Election Data Form to enter all applicable
            rule selections here.

        2.  Depending on the required settings you enter, some options
            may remain grayed out and unselectable by the user. This is
            normal and should be expected, as all fields may not require
            input.

        3.  If there are any questions or concerns about the information
            needed to complete this tab, STOP and consult your
            controlling governing body for additional guidance about the
            jurisdiction’s particular ranked choice voting requirements.

    6.  Voter Error Rules

        1.  Use the completed Election Data Form to enter all applicable
            rules selections here.

        2.  Depending on the required settings you enter, some options
            may remain grayed out and unselectable by the user. This is
            normal and should be expected, as all fields may not require
            input.

        3.  If there are any questions or concerns about the information
            needed to complete this tab, STOP and consult your
            controlling governing body for additional guidance about the
            jurisdiction’s particular ranked choice voting requirements.

    7.  Output Tab

        1.  In the output directory field, enter the file path to your
            election folder set up in 3a. This will place all tabulation
            files in this folder once the tabulation is complete.

        2.  If you are tabulating by precinct, check the box labeled
            “Tabulate by Precinct.”

        3.  If you are generating a CDF JSON, check the box labeled
            Generate CDF JSON. Generally, this setting will be left
            unchecked as it is not required in most tabulations.

2.  **Validating the Configuration File**

    1.  Click on “Tabulation” at the top of the software window.

    2.  Click on “Validate”

    3.  Refer to the log box at the bottom of the application. If the
        message “Contest config validation successful.” appears, your
        contest configuration has been successfully completed.

    4.  If any error messages appear in the log box, refer to **RCTab
        v1.3.2 Section 29 - RCTab Operator Log Messages v.1.2.2** and
        messages in the log box for how to resolve errors. If the error
        persists, restart the RCTab software.

3.  **Saving the Configuration File**

    1.  Click on “File” at the top of the software window.

    2.  Click on “Save…”

    3.  Select a location to save the configuration file. The
        manufacturer suggests users save the configuration file to the
        same location set in the Output Directory setting.

    4.  Refer to the log box at the bottom of the application. If the
        message “Successfully saved file: Filepath” your configuration
        .json file has been successfully saved.

    5.  If any error messages appear in the log box, refer to **RCTab
        v1.3.2 Section 29 - RCTab Operator Log Messages v.1.2.2**
        documentation and messages in the log box for how to resolve
        errors. If the error persists, restart the RCTab software.

4.  **Once a configuration is saved, the user is ready to run a
    tabulation.**

    1.  Click on “Tabulation” at the top of the software window.

    2.  Click on “Tabulate”

    3.  Tabulation will begin.

    4.  If all the above steps were successfully completed, tabulation
        will run until complete.

    5.  The Tabulator log box will update with messages as Tabulation
        proceeds.

    6.  Once complete, the Tabulator log box will display a message
        stating, “Results written to: \[filepath from Output
        Directory\]”

5.  **Output files will be:**

    1.  .csv contest summary files

        1.  Whole-contest summary files

    2.  .json contest summary files

        1.  Whole-contest summary files

    3.  .log audit files

        1.  .log audit files are exported in 50MB sections. If a .log
            file exceeds 50MB, an additional .log file is started by
            RCTab.

    4.  .hash files

        1.  Cryptographic hashes of the output files above

6.  Once output files have been generated, users should compare the
    results to the known test deck outcome prepared for the current
    election. If the results do not match, the settings entered into
    RCTab should be confirmed with officials that the configuration
    settings are in compliance with law and policy set by the user
    jurisdiction. Users should also confirm the known outcome from the
    test deck is, in fact, correct, and the voting system used to
    tabulate the ballots is also working properly. The test deck should
    be counted by hand BEFORE the actual L&A testing begins on both the
    user jurisdiction’s voting system and RCTab. This includes a
    headcount of the round-by-round RCV outcomes as well.

7.  Users can then navigate to “File” and click “Exit” if all contests
    are tabulated.

8.  <span class="mark">Users can verify result file hashes by following
    the instructions in **RCTab v.1.3.1 Section 23 - Trusted Build &
    Output Hash Verification - Windows OS v.1.2.2**</span>

9.  If any errors arise in the use of RCTab, refer to the relevant
    documentation for the source of the error. RCTab errors should refer
    to **RCTab v1.3.2 Section 29 - RCTab Operator Log Messages
    v.1.2.2**. Errors arising out of any hardware or software other than
    RCTab should refer to the **RCTab v1.3.2 Section 09 - System
    Maintenance Manual v.1.3.2** and any relevant user and maintenance
    manual.

10. We recommend that the configuration file be set up fully each time
    the user accesses RCTab. Starting a new configuration file each time
    will lessen the chances that outdated CVRs or other data is
    inadvertently introduced into the system.

**Using Preloaded Sample CVRs to perform L&A**

-   As part of the download, RCTab includes three test CVR files along
    with configuration files and summary results. To test, the user
    access RCTab and should do the following:

    -   Access the sample\_input folder. This folder is part of the
        installation files created on the computer when RCTab was
        installed.

    -   Choose the 2015\_Portland\_Mayor folder

    -   Load the file 2015\_portland\_mayor\_config.json

    -   Load the file 2015\_portalnd\_mayor\_cvr.xlsx

    -   Validate the configuration file and run the tabulation

    -   Compare the results to the summary file
        “2015\_portland\_mayor\_expected\_summary.json” contained below.
        If results do not match, the user should confirm the correct
        configuration and CVR files were correctly selected and repeat
        the test. Any irreconcilable issues should be reported to the
        RCVRC for further examination.

> 2015\_portland\_mayor\_expected\_summary.pdf
>
> { 

 "config" : { 

 "contest" : "Portland 2015 Mayoral Race",  "date" : "2015-11-03", 

 "jurisdiction" : "Portland, ME",  "office" : "Mayor", 

 "threshold" : "48" 

 }, 

 "results" : \[ { 

 "round" : 1, 

 "tally" : { 

 "Bragdon, Charles E." : "11",  "Brennan, Michael F." : "6",  "Bryant,
Peter G." : "9", 

 "Carmona, Ralph C." : "12", 

 "Dodge, Richard A." : "10", 

 "Duson, Jill C." : "5", 

 "Eder, John M." : "3", 

 "Haadoow, Hamza A." : "8", 

 "Lapchick, Jodie L." : "7", 

 "Marshall, David A." : "2", 

 "Mavodones, Nicholas M. Jr." : "13",  "Miller, Markos S." : "3", 

 "Rathband, Jed" : "2", 

 "Strimling, Ethan K." : "1",  "Undeclared Write-ins" : "0",  "Vail,
Christopher L." : "6"  }, 

 "tallyResults" : \[ { 

 "eliminated" : "Undeclared Write-ins",  "transfers" : { } 

 }, { 

 "eliminated" : "Strimling, Ethan K.",  "transfers" : { 

 "Vail, Christopher L." : "1"  } 

 } \] 

 }, { 

 "round" : 2, 

 "tally" : { 

 "Bragdon, Charles E." : "11",  "Brennan, Michael F." : "6",  "Bryant,
Peter G." : "9", 

 "Carmona, Ralph C." : "12", 

 "Dodge, Richard A." : "10", 

 "Duson, Jill C." : "5", 

 "Eder, John M." : "3", 

 "Haadoow, Hamza A." : "8", 

 "Lapchick, Jodie L." : "7", 

 "Marshall, David A." : "2", 

 "Mavodones, Nicholas M. Jr." : "13",  "Miller, Markos S." : "3", 

 "Rathband, Jed" : "2", 

 "Vail, Christopher L." : "7"  }, 

 "tallyResults" : \[ {

 "eliminated" : "Rathband, Jed",  "transfers" : { 

 "Mavodones, Nicholas M. Jr." : "2"  } 

 } \] 

 }, { 

 "round" : 3, 

 "tally" : { 

 "Bragdon, Charles E." : "11",  "Brennan, Michael F." : "6",  "Bryant,
Peter G." : "9", 

 "Carmona, Ralph C." : "12",  "Dodge, Richard A." : "10",  "Duson, Jill
C." : "5", 

 "Eder, John M." : "3", 

 "Haadoow, Hamza A." : "8",  "Lapchick, Jodie L." : "7",  "Marshall,
David A." : "2",  "Mavodones, Nicholas M. Jr." : "15",  "Miller, Markos
S." : "3",  "Vail, Christopher L." : "7"  }, 

 "tallyResults" : \[ { 

 "eliminated" : "Marshall, David A.",  "transfers" : { 

 "Miller, Markos S." : "2"  } 

 } \] 

 }, { 

 "round" : 4, 

 "tally" : { 

 "Bragdon, Charles E." : "11",  "Brennan, Michael F." : "6",  "Bryant,
Peter G." : "9", 

 "Carmona, Ralph C." : "12",  "Dodge, Richard A." : "10",  "Duson, Jill
C." : "5", 

 "Eder, John M." : "3", 

 "Haadoow, Hamza A." : "8",  "Lapchick, Jodie L." : "7",  "Mavodones,
Nicholas M. Jr." : "15",  "Miller, Markos S." : "5",  "Vail, Christopher
L." : "7"  }, 

 "tallyResults" : \[ { 

 "eliminated" : "Eder, John M.",  "transfers" : { 

 "Duson, Jill C." : "3" 

 } 

 } \] 

 }, { 

 "round" : 5, 

 "tally" : { 

 "Bragdon, Charles E." : "11",  "Brennan, Michael F." : "6",  "Bryant,
Peter G." : "9", 

 "Carmona, Ralph C." : "12",

 "Dodge, Richard A." : "10",  "Duson, Jill C." : "8", 

 "Haadoow, Hamza A." : "8", 

 "Lapchick, Jodie L." : "7",  "Mavodones, Nicholas M. Jr." : "15", 
"Miller, Markos S." : "5", 

 "Vail, Christopher L." : "7"  }, 

 "tallyResults" : \[ { 

 "eliminated" : "Miller, Markos S.",  "transfers" : { 

 "Mavodones, Nicholas M. Jr." : "5"  } 

 } \] 

 }, { 

 "round" : 6, 

 "tally" : { 

 "Bragdon, Charles E." : "11",  "Brennan, Michael F." : "6",  "Bryant,
Peter G." : "9", 

 "Carmona, Ralph C." : "12",  "Dodge, Richard A." : "10",  "Duson, Jill
C." : "8", 

 "Haadoow, Hamza A." : "8", 

 "Lapchick, Jodie L." : "7",  "Mavodones, Nicholas M. Jr." : "20", 
"Vail, Christopher L." : "7"  }, 

 "tallyResults" : \[ { 

 "eliminated" : "Brennan, Michael F.",  "transfers" : { 

 "Bragdon, Charles E." : "3",  "Bryant, Peter G." : "2",  "Carmona,
Ralph C." : "1"  } 

 } \] 

 }, { 

 "round" : 7, 

 "tally" : { 

 "Bragdon, Charles E." : "14",  "Bryant, Peter G." : "11", 

 "Carmona, Ralph C." : "13",  "Dodge, Richard A." : "10",  "Duson, Jill
C." : "8", 

 "Haadoow, Hamza A." : "8", 

 "Lapchick, Jodie L." : "7",  "Mavodones, Nicholas M. Jr." : "20", 
"Vail, Christopher L." : "7"  }, 

 "tallyResults" : \[ { 

 "eliminated" : "Vail, Christopher L.",  "transfers" : { 

 "Mavodones, Nicholas M. Jr." : "6",  "exhausted" : "1" 

 } 

 } \] 

 }, {

 "round" : 8, 

 "tally" : { 

 "Bragdon, Charles E." : "14",  "Bryant, Peter G." : "11",  "Carmona,
Ralph C." : "13",  "Dodge, Richard A." : "10",  "Duson, Jill C." : "8", 

 "Haadoow, Hamza A." : "8",  "Lapchick, Jodie L." : "7",  "Mavodones,
Nicholas M. Jr." : "26"  }, 

 "tallyResults" : \[ { 

 "eliminated" : "Lapchick, Jodie L.",  "transfers" : { 

 "Haadoow, Hamza A." : "1",  "Mavodones, Nicholas M. Jr." : "6"  } 

 } \] 

 }, { 

 "round" : 9, 

 "tally" : { 

 "Bragdon, Charles E." : "14",  "Bryant, Peter G." : "11",  "Carmona,
Ralph C." : "13",  "Dodge, Richard A." : "10",  "Duson, Jill C." : "8", 

 "Haadoow, Hamza A." : "9",  "Mavodones, Nicholas M. Jr." : "32"  }, 

 "tallyResults" : \[ { 

 "eliminated" : "Duson, Jill C.",  "transfers" : { 

 "Bryant, Peter G." : "1",  "Dodge, Richard A." : "5",  "Mavodones,
Nicholas M. Jr." : "2"  } 

 } \] 

 }, { 

 "round" : 10, 

 "tally" : { 

 "Bragdon, Charles E." : "14",  "Bryant, Peter G." : "12",  "Carmona,
Ralph C." : "13",  "Dodge, Richard A." : "15",  "Haadoow, Hamza A." :
"9",  "Mavodones, Nicholas M. Jr." : "34"  }, 

 "tallyResults" : \[ { 

 "eliminated" : "Haadoow, Hamza A.",  "transfers" : { 

 "Carmona, Ralph C." : "2",  "Mavodones, Nicholas M. Jr." : "7"  } 

 } \] 

 }, { 

 "round" : 11, 

 "tally" : {

 "Bragdon, Charles E." : "14", 

 "Bryant, Peter G." : "12", 

 "Carmona, Ralph C." : "15", 

 "Dodge, Richard A." : "15", 

 "Mavodones, Nicholas M. Jr." : "41"  }, 

 "tallyResults" : \[ { 

 "eliminated" : "Bryant, Peter G.",  "transfers" : { 

 "Bragdon, Charles E." : "9",  "Carmona, Ralph C." : "2", 

 "Mavodones, Nicholas M. Jr." : "1"  } 

 } \] 

 }, { 

 "round" : 12, 

 "tally" : { 

 "Bragdon, Charles E." : "23", 

 "Carmona, Ralph C." : "17", 

 "Dodge, Richard A." : "15", 

 "Mavodones, Nicholas M. Jr." : "42"  }, 

 "tallyResults" : \[ { 

 "eliminated" : "Dodge, Richard A.",  "transfers" : { 

 "Bragdon, Charles E." : "1",  "Carmona, Ralph C." : "11", 

 "Mavodones, Nicholas M. Jr." : "3"  } 

 } \] 

 }, { 

 "round" : 13, 

 "tally" : { 

 "Bragdon, Charles E." : "24", 

 "Carmona, Ralph C." : "28", 

 "Mavodones, Nicholas M. Jr." : "45"  }, 

 "tallyResults" : \[ { 

 "eliminated" : "Bragdon, Charles E.",  "transfers" : { 

 "Carmona, Ralph C." : "12", 

 "Mavodones, Nicholas M. Jr." : "9",  "exhausted" : "3" 

 } 

 } \] 

 }, { 

 "round" : 14, 

 "tally" : { 

 "Carmona, Ralph C." : "40", 

 "Mavodones, Nicholas M. Jr." : "54"  }, 

 "tallyResults" : \[ { 

 "elected" : "Mavodones, Nicholas M. Jr.",  "transfers" : { } 

 } \] 

 } \] 

> }

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
<th>09/28/2023</th>
<th>1.3.2</th>
<th>Update hash verification of output with .hash files</th>
<th>Mathew Ruberg</th>
</tr>
<tr class="odd">
<th>04/28/2023</th>
<th>1.3.1</th>
<th>Updated for RCTab v1.3.2</th>
<th>Melissa Hall</th>
</tr>
<tr class="header">
<th>04/04/2023</th>
<th>1.3.0</th>
<th>Edits for clarity</th>
<th>Rene Rojas</th>
</tr>
<tr class="odd">
<th>01/14/2022</th>
<th>1.2.0</th>
<th>Updated Names and edits for clarity</th>
<th>Ryan Kirby</th>
</tr>
<tr class="header">
<th>05/14/2021</th>
<th>1.1.0</th>
<th><ul>
<li><p>Updated to add information regarding creation of L&amp;A test
decks in steps 2 &amp; 11</p></li>
<li><p>Added procedures for using sample CVRs to perform
L&amp;A</p></li>
</ul></th>
<th>Rosemary F. Blizzard</th>
</tr>
<tr class="odd">
<th>04/27/2021</th>
<th>1.0.0</th>
<th>L&amp;A Testing Procedures</th>
<th>Rosemary F. Blizzard</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

