# RCTab v1.3.2 Section 05 - Acceptance Test Procedures v.1.2.1

> **RCTab v1.3.2 Section 05 - Acceptance Test Procedures v.1.2.1**
> document is solely for use in the State of California. This document
> can be expanded or updated as is necessary or required. Where
> relevant, this document refers to specific sections and requirements
> of the California Voting System Standards. Any recommendations listed
> in this document should not supersede user jurisdiction procedures or
> other controlling governance entities.

**This Acceptance Test is designed to verify that RCTab is correctly
configured and operating properly. Acceptance tests should always be
conducted on a new install of RCTab prior to its use in an election.
Also, acceptance tests should be conducted when there are significant
changes or repairs made to the hardware where the software is installed.
Example: If the RCTab computer is sent out for repair or if a new
version of the operating system or RCTab is installed, an acceptance
test should be conducted on RCTab as soon as it is returned.**

## Required Materials

In addition to the following, you must know the correct version of your
operating system and RCTab. The manufacturer can provide assistance in
determining the correct version. Users can also find that information in
**RCTab v1.3.2 Section 22 - Installation Instructions for Windows OS
v.1.3.0** documentation. The following components of a voting system are
required to complete the acceptance testing procedures:

1.  One Computer with RCTab installed. Installation instructions can be
    found in **RCTab v1.3.2 Section 22 - Installation Instructions for
    Windows OS v.1.3.0** document.

2.  Recommended battery backup if the user jurisdiction facility does
    not have generator capabilities in the event of a power failure.

The RCTab computer system, consisting of items 1 & 2 above, should have
been set up and tested for functionality before beginning this test. IT
staff may do this part of the setup if properly supervised by election
staff who have been trained in accordance with procedures laid out in
the personnel documentation. Requirements are included in **RCTab v1.3.2
Section 03 - System Hardware Specification v.1.1.1** and in **RCTab
v1.3.2 Section 16 - System Hardening Procedures - Windows OS v.1.3.0.**

## Verify RCTab Hardware

Verify that all hardware components of the RCTab computer are turned on
and functioning properly. This includes checks of the printer(s), flash
drives, USB port, etc.

## Verify Correct Operating System and RCTab Software

Turn on the RCTab computer. As the computer boots up, verify that the
correct versions of the operating system and the RCTab software are
installed. Compute the hash codes for the RCTab voting system software
and compare them with the hash value provided with the trusted build.
See **RCTab v1.3.2 Section 03 - System Hardware Specification v.1.1.1.**
document and **RCTab v1.3.2 Section 22 - Installation Instructions for
Windows OS v.1.3.0** document for procedures and more information.

Manufacturer recommendation: When your election management system
successfully passes the acceptance test, affix to the top of the
computer case a label with the date of the test and the initials of the
person conducting the test. This information should also be recorded on
the access log as per the user jurisdiction’s security guidelines and
protocols.

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
<th>04/28/2023</th>
<th>1.2.1</th>
<th>Updated to Reflect RCTab v.1.3.1</th>
<th>Sam Prescott</th>
</tr>
<tr class="odd">
<th>04/04/2023</th>
<th>1.2.0</th>
<th>Updated to Reflect RCTab v.1.3.0</th>
<th>Rene Rojas</th>
</tr>
<tr class="header">
<th>1/12/2022</th>
<th>1.1.0</th>
<th>Revisions for RCTab &amp; Clarity</th>
<th>Ryan Kirby</th>
</tr>
<tr class="odd">
<th>04/26/2021</th>
<th>1.0.0</th>
<th>Acceptance Testing Procedures</th>
<th>Rosemary F. Blizzard</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

# [RCTab v1.3.2 Section 06 - System Design Specifications v.1.1.1](#rctab-v1.3.2-section-06---system-design-specifications-v.1.1.1)

> **[RCTab v1.3.2 Section 06 - System Design Specifications
> v.1.1.1](#rctab-v1.3.2-section-06---system-design-specifications-v.1.1.1)**
> document is solely for use in the State of California. This document
> can be expanded or updated as is necessary or required. Where
> relevant, this document refers to specific sections and requirements
> of the California Voting System Standards. Any recommendations listed
> in this document should not supersede user jurisdiction procedures or
> other controlling governance entities.

5.1.1 - Configuration of software, both operating systems and
applications, is critical to proper system functioning. Correct test
design and sufficient test execution must account for the intended and
proper configuration of all system components. Therefore, the vendors
shall submit a record of all user selections made during software
installation as part of the Technical Data Package. The vendor shall
also submit a record of all configuration changes made to the software
following its installation. The accredited test lab shall confirm the
propriety and correctness of these user selections and configuration
changes.

-   RCTab software does not permit installation configuration changes by
    the user at any time during or after the installation process.

-   RCTab software installation must be conducted according to **RCTab
    v1.3.2 Section 22 - Installation Instructions for Windows OS
    v.1.3.0.**

-   See **<span class="mark">RCTab v1.3.2 Section 08 - System Operations
    Procedures v.1.2.2</span>** and **<span class="mark">RCTab v1.3.2
    Section 16 - System Hardening Procedures - Windows OS
    v.1.3.0</span>** document for more information about RCTab operation
    and other software requirements on hardware used for RCTab software.

5.3.a - Maintain the integrity of voting and audit data during an
election, and for at least 22 months thereafter, a time sufficient in
which to resolve most contested elections and support other activities
related to the reconstruction and investigation of a contested election.

-   The user jurisdiction should follow all record retention policy
    requirements set by the jurisdiction itself and all other higher
    governing authorities with respect to the appropriate retention and
    storage of any materials generated by the software.

-   The manufacturer recommends that in the absence of any user
    controlling records retention schedule, all materials generated by
    the software for a particular election be securely stored for a
    minimum period of 22 months.

-   User must save all results files created for each contest in an
    election, including all configuration files used for those contests.
    This should also include testing data. User must export files to a
    hard drive secured and stored according to jurisdiction policies.
    For every contest processed through RCTab at least four files must
    be exported:

    -   Contest configuration.json file

    -   Contest summary .csv file

    -   Contest summary .json file

    -   Contest audit .log file(s)

        -   Depending on the size of the contest multiple .log files may
            be created. .log files have a maximum size of 50MB. Large
            contests frequently have many .log files.

    -   The **<span class="mark">RCTab v1.3.2 Section 08 - System
        Operations Procedures v.1.2.2</span>** suggests users create a
        folder for each contest where all relevant files for the contest
        are saved. If these procedures are followed, users will be able
        to copy whole sets of contest files over, folder by folder, on
        the external hard drive to be used for records retention.

    -   At the close of an election, users must also export any operator
        .log files, located in the bin folder of the RCTab installation
        folder. These .log files will be named rcv\_0.log, rcv\_1.log,
        and so on, depending upon how many .log files were produced
        during use of RCTab. These files must also be placed on the
        secured hard drive.

    -   After successfully copying over all relevant files to a secured
        hard drive, the hard drive should be stored in a temperature and
        humidity controlled area that meets the criteria for such media.

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
<th>04/27/2023</th>
<th>1.1.1</th>
<th>Updated for RCTab v1.3.1</th>
<th>Kelly Sechrist</th>
</tr>
<tr class="odd">
<th>01/12/2022</th>
<th>1.1.0</th>
<th>Revisions to Match RCTab Branding</th>
<th>Ryan Kirby</th>
</tr>
<tr class="header">
<th>04/24/2021</th>
<th>1.0.0</th>
<th>System Design Specification</th>
<th>Rosemary F. Blizzard</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

