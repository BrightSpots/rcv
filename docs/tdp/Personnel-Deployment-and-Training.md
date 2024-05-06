# RCTab v1.3.2 Section 09 - System Maintenance Manual v.1.3.2

> **RCTab v1.3.2 Section 09 - System Maintenance Manual v.1.3.2**
> document is solely for use in the State of California. This document
> can be expanded or updated as is necessary or required. Where
> relevant, this document refers to specific sections and requirements
> of the California Voting System Standards. Any recommendations listed
> in this document should not supersede user jurisdiction procedures or
> other controlling governance entities.

RCTab leverages content from the jurisdictions voting system, all
maintenance on equipment should be referred to your voting system
vendor. All RCTab hardware is COTS. All software other than RCTab
software itself is also COTS. Refer to a COTS Maintenance Manuals for
maintenance procedures.

Maintenance of RCTab software: Obtain an encrypted version of the
trusted build from the governing body of your jurisdiction or the VSTL.
Maintain that original copy in a secure, temperature- and
humidity-controlled environment. Label it with the system version and
hash value.

Recommended service actions to correct malfunctions or problems shall be
discussed, along with personnel and expertise required to repair and
maintain the system; and equipment, materials, and facilities needed for
proper maintenance. This manual shall include the sections listed below.

## 9.9.1. Introduction

The manufacturer shall describe the structure and function of the
equipment (and related software) for election preparation, programming,
vote recording, tabulation, and reporting in sufficient detail to
provide an overview of the system for maintenance and for identification
of faulty hardware or software.

-   Election preparation:

    -   The software can be used to set up jurisdiction configurations
        according to the ranked choice voting rules used in the
        jurisdiction’s RCV election(s). Tabulation rules options are
        menu selectable using the GUI. See also:

> **RCTab v1.3.2 Section 08 - System Operations Procedures v.1.2.2**
>
> **RCTab v1.3.2 Section 11 - L&A Testing v.1.3.2**
>
> **RCTab v1.3.2 Section 25 - Configuration File Parameters v.1.1.1**

-   Programming

    -   RCTab is not used for programming elections

-   Vote Recording

    -   RCTab processes RCV election data according to rules users input
        in configuration files. See also ***RCTab v1.3.2 Section 08 -
        System Operations Procedures v.1.2.2***.

    -   If users have issues with CVR files, refer to CVR procedures
        from jurisdiction and relevant procedures in ***RCTab v1.3.2
        Section 08 - System Operations Procedures v.1.2.2*.**

-   Tabulation

    -   Tabulator is used solely with properly adjudicated CVR export
        files from the user jurisdiction and user jurisdiction’s primary
        voting system vendor.

-   Reporting

    -   RCTab reports out summary results files in .csv and .json
        formats as well as an audit log in .log format. See also **RCTab
        v1.3.2 Section 02 - Software Design and Specifications
        v.1.4.2**.

The description shall include a concept of operations that fully
describes such items as:

1.  The electrical and mechanical functions of the equipment

    1.  RCTab runs using the power from the hardware referenced in
        **RCTab v1.3.2 Section 03 - System Hardware Specification
        v.1.1.1**.

2.  How the processes of ballot handling and reading are performed
    (paper-based systems)

    1.  RCTab does not handle or read ballots.

3.  How vote selection and casting of the ballot are performed (DRE
    systems);

    1.  RCTab does not handle vote selection or ballot casting.

4.  How transmission of data over a network is performed (DRE systems,
    where applicable)

    1.  RCTab does not transmit data over a network.

5.  How data are handled in the processor and memory units

    1.  Data are loaded from disk, stored in RAM, then processed in the
        CPU, periodically writing the data back to RAM and to disk.

6.  How data output is initiated and controlled

    1.  In the output tab within RCTab, users can select the path for
        all RCTab output files. Output files will be .csv contest
        summary files, .json contest summary files, .log audit files and
        corresponding .hash files Optionally, users can configure RCTab
        to export .json CDF (common data format) files if “Generate a
        CDF JSON” is checked. See **RCTab v1.3.2 Section 18 - User Guide
        v.1.2.2** for additional information.  
          
        When installed using the instructions in **RCTab v1.3.2 Section
        22 - Installation Instructions for Windows OS v.1.3.0** RCTab
        output is read-only and cannot be modified or deleted. The
        contents of the output can also be verified cryptographically
        with their corresponding .hash using the instructions in RCTab
        v.1.3.1 Section 23 - Trusted Build & Output Hash Verification -
        Windows OS v.1.2.2

7.  How power is converted or conditioned

    1.  Power comes from the hardware referenced in **RCTab v1.3.2
        Section 03 - System Hardware Specification v.1.1.1**.

8.  How test and diagnostic information is acquired and used

    1.  See **RCTab v1.3.2 Section 11 - L&A Testing v.1.3.2**

## 9.9.2 Maintenance Procedures

The manufacturer shall describe preventive and corrective maintenance
procedures for hardware and software.

### 9.9.2.1 Preventative Maintenance Procedures

The manufacturer shall identify and describe:

1.  All required and recommended preventive maintenance tasks, including
    software tasks such as software backup, database performance
    analysis, and database tuning

    1.  Confirm that you are using the most recent version of the
        software. Confirm this by reviewing the black log box at the
        bottom of RCTab, as seen in the screenshot below. The second
        line of text will read “Welcome to RCTab version \[version
        number\].” Confirm that this version number matches up with the
        version number required of your jurisdiction. This system’s
        number will be version 1.3.2.

    2.  All ballot adjudication must be completed prior to using CVR
        file(s) with RCTab. Such review must ensure the configuration
        file conforms to candidate names, undervote, overvote, and other
        labels used in CVR files. See also **RCTab v1.3.2 Section 08 -
        System Operations Procedures v.1.2.2**.

2.  Number and skill levels of personnel required for each task

    1.  Minimum of 2. ( in compliance with the jurisdiction’s
        guidelines/rules regarding partisan participation in tabulation
        functions). Skill level: basic knowledge of how to interact with
        a desktop application.

3.  Parts, supplies, special maintenance equipment, software tools, or
    other resources needed for maintenance

    1.  Recommend backup computer as well as backup USB stick stored in
        accordance with user jurisdiction security policies. See also:

> **RCTab v1.3.2 Section 03 - System Hardware Specification v.1.1.1,**
>
> **RCTab v1.3.2 Section 16 - System Hardening Procedures - Windows OS
> v.1.3.0,**
>
> **RCTab v1.3.2 Section 22 - Installation Instructions for Windows OS
> v.1.3.0**

1.  Any maintenance tasks that must be coordinated with the manufacturer
    or a third party (such as coordination that may be needed for
    off-the-shelf items used in the system)

    1.  The manufacturer will confirm with user jurisdiction two months
        prior, or as time permits, to an election in which RCTab will be
        used that the user jurisdiction is using the most recently
        certified version of the software.

    2.  User jurisdiction must review relevant state and local laws
        regulating ranked choice voting elections. Ensure that
        configuration files produced in the use of RCTab conform to
        relevant laws. See **RCTab v1.3.2 Section 08 - System Operations
        Procedures v.1.2.2** for more.

    3.  User jurisdiction must review cast-vote record file formats from
        the voting system vendor providing CVRs for use with RCTab.

### 9.9.2.2.Corrective Maintenance Procedures

The manufacturer shall provide fault detection, fault isolation,
correction procedures, and logic diagrams for all operational
abnormalities identified by design analysis and operating experience.

The manufacturer shall identify specific procedures to be used in
diagnosing and correcting problems in the system hardware (or
user-controlled software). Descriptions shall include:

1.  Steps to replace failed or deficient equipment

    1.  Use **RCTab v1.3.2 Section 29 - RCTab Operator Log Messages
        v.1.2.2** and information provided by Operator Log Box to
        determine if the error has a known correction.

2.  Steps to correct deficiencies or faulty operations in software

    1.  Use **RCTab v1.3.2 Section 29 - RCTab Operator Log Messages
        v.1.2.2** and information provided by Operator Log Box to
        determine if the error has a known correction.

3.  Modifications that are necessary to coordinate any modified or
    upgraded software with other software modules

    1.  Any additional software should conform to the requirements
        described in this document and in **RCTab v1.3.2 Section 16 -
        System Hardening Procedures - Windows OS v.1.3.0**.

4.  The number and skill levels of personnel needed to accomplish each
    procedure

    1.  Minimum of 2 (in compliance with the jurisdiction’s
        guidelines/rules regarding partisan participation in tabulation
        functions). Skill level: knowledge of how to retrieve and
        install RCTab software from a trusted source (requires one-hour
        training).

5.  Special maintenance equipment, parts, supplies, or other resources
    needed to accomplish each procedure

    1.  Recommend backup Tabulator computer as well as backup Tabulator
        USB stick stored in accordance with user jurisdiction security
        policies. See also:

> **RCTab v1.3.2 Section 03 - System Hardware Specification v.1.1.1,**
>
> **RCTab v1.3.2 Section 16 - System Hardening Procedures Windows OS -
> v.1.2.1,**
>
> **RCTab v1.3.2 Section 22 - Installation Instructions for Windows OS
> v.1.2.0**

1.  Any coordination required with the manufacturer, or other party, for
    off the shelf items

    1.  If no known correction, contact the manufacturer to review
        procedures already completed by the jurisdiction and propose
        resolution steps.

## 9.9.3 Maintenance Equipment

The manufacturer shall identify and describe a special purpose test or
maintenance equipment recommended for fault isolation and diagnostic
purposes.

-   JUnit Jupiter 5.6.2 and JUnit Platform 1.6.2 are used for all
    automated testing of RCTab. See also **RCTab v1.3.2 Section 03 -
    System Hardware Specification v.1.1.1.**

## 9.9.4 Parts and Materials 

Manufacturers shall provide detailed documentation of parts and
materials needed to operate and maintain the system. Additional
requirements apply for paper-based systems.

### 9.9.4.1. Common Standards

The manufacturer shall provide a complete list of approved parts and
materials needed for maintenance.

-   A computer conforming to the requirements laid out in **RCTab v1.3.2
    Section 03 - System Hardware Specification v.1.1.1.**

-   A backup USB drive with a copy of certified voting system software
    retrieved from a trusted source.

### 9.9.4.2 Paper-based Systems 

RCTab is entirely software based. It does not rely upon any insertion of
paper to produce results. This section does not apply to RCTab.

## 9.9.5 Maintenance Facilities and Support 

The manufacturer shall identify all facilities, furnishings, fixtures,
and utilities that will be required for equipment maintenance. In
addition, manufacturers shall specify the assumptions made with regard
to any parameters that impact the mean time to repair. These factors
shall include at a minimum:

1.  Recommended number and locations of spare devices or components to
    be kept on hand for repair purposes during periods of system
    operation

    1.  Recommend backup Tabulator computer as well as backup Tabulator
        USB stick stored in accordance with user jurisdiction security
        policies. See also:

> **RCTab v1.3.2 Section 03 - System Hardware Specification v.1.1.1,**
>
> **RCTab v1.3.2 Section 16 - System Hardening Procedures - Windows OS -
> v.1.2.1,**
>
> **RCTab v1.3.2 Section 22 - Installation Instructions for Windows OS
> v.1.3.0**

1.  Recommended number and locations of qualified maintenance personnel
    who need to be available to support repair calls during system
    operation

    1.  1-2 qualified personnel. Personnel who have been adequately
        trained, as required above, may serve as maintenance personnel.
        Recommend at least one person available in or near the
        tabulation location. Manufacturer personnel can be made
        available for support and consultation both via phone/virtually
        and in-person.

2.  Organizational affiliation (i.e., jurisdiction, manufacturer) of
    qualified maintenance personnel

    1.  Staff from the user jurisdiction should maintain the system.

## 9.9.6 Appendices

The manufacturer may provide descriptive material and data supplementing
the various sections of the body of the System Maintenance Manual. The
content and arrangement of appendices shall be at the discretion of the
manufacturer. Topics recommended for amplification or treatment in the
appendix include:

Glossary: A listing and brief definition of all terms that may be
unfamiliar to persons not trained in either voting systems or computer
maintenance;

-   See **RCTab v1.3.2 Section 08 - System Operations Procedures
    v.1.2.2** for all relevant terms used in RCTab.

-   See **RCTab v1.3.2 Section 25 - Configuration File Parameters
    v.1.1.1** for details on all parameters in RCTab software.

References: A list of references to all manufacturer documents and other
sources related to maintenance of the system;

-   **RCTab v1.3.2 Section 03 - System Hardware Specification v.1.1.1**

-   **RCTab v1.3.2 Section 08 - System Operations Procedures v.1.2.2**

-   **RCTab v1.3.2 Section 16 - System Hardening Procedures - Windows OS
    v.1.3.0**

-   **RCTab v1.3.2 Section 25 - Configuration File Parameters v.1.1.1**

Detailed Examples: Detailed scenarios that outline correct system
responses to every conceivable faulty operator input; alternative
procedures may be specified depending on the system state.

-   **RCTab v1.3.2 Section 29 - RCTab Operator Log Messages v.1.2.2**

Maintenance and Security Procedures: This appendix shall contain
technical illustrations and schematic representations of electronic
circuits unique to the system.

-   No unique electronic circuits. No such illustrations are relevant.

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
<th>Includes read-only output and .hash file verification in
9.9.1.f</th>
<th>Mathew Ruberg</th>
</tr>
<tr class="odd">
<th>04/18/2023</th>
<th>1.3.1</th>
<th>Updated to reflect v.1.3.1</th>
<th>Ryan Kirby</th>
</tr>
<tr class="header">
<th>04/18/2023</th>
<th>1.3.0</th>
<th>Changed to reflect CA Voting System Standards</th>
<th>Ryan Kirby</th>
</tr>
<tr class="odd">
<th>04/04/2023</th>
<th>1.2.0</th>
<th>Updated to Reflect RCTab v.1.3.0</th>
<th>Rene Rojas</th>
</tr>
<tr class="header">
<th>01/15/2022</th>
<th>1.1.0</th>
<th>Revisions for clarity</th>
<th>Rene Rojas</th>
</tr>
<tr class="odd">
<th>04/27/2021</th>
<th>1.0.0</th>
<th>System Maintenance Procedures</th>
<th>Chris Hughes</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

# 

