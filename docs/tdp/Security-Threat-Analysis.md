# RCTab v1.3.2 Section 07K - Development Environment Specification v.1.0.1

> **RCTab v1.3.2 Section 07K - Development Environment Specification
> v.1.0.1** document is solely for use in the State of California. This
> document can be expanded or updated as is necessary or required. Where
> relevant, this document refers to specific sections and requirements
> of the California Voting System Standards. Any recommendations listed
> in this document should not supersede user jurisdiction procedures or
> other controlling governance entities.

**Objective**: This document shall provide descriptions of the physical,
personnel, procedural, and technical security of the development
environment, including version control, tools used, coding standards
used, software engineering model used, and description of developer and
independent testing.

**Version Control**: We use Git version control software (git-scm.com)
in conjunction with Github (github.com/BrightSpots/rcv) to coordinate
the efforts of our developers and maintain a complete record of all
software code changes to RCTab and the reasoning behind them.

**Coding Standards**: We use *Google Checkstyle for Java* as our
published, reviewed, and industry-accepted code style. For details, see
[<u>Google Java Style
Guide</u>](https://google.github.io/styleguide/javaguide.html) and page
93 of the [<u>VVSG Volume
1.0</u>](https://www.eac.gov/sites/default/files/eac_assets/1/28/VVSG.1.0_Volume_1.PDF)
guide.

**Software Engineering Model**: RCTab development uses the Waterfall
model.

**Description of Developer**: The Ranked Choice Voting Resource Center
(RCVRC), a nonprofit organization, and Bright Spots, a software
development team, have joined together to develop RCTab as an
open-source software package that provides post-election tabulation to
determine results in a ranked choice voting election.

**Independent Testing**: We have developed a suite of 68 Tabulation
Regression Tests and continue to add more tests as new features and bug
fixes are added. These are designed to verify that various aspects of
Tabulator functionality behave as expected. They also verify that new
code changes do not inadvertently alter Tabulator behavior. The entire
test suite must be run, and all tests must pass before any new code
changes can be merged into the main Tabulator repository. See **RCTab
v1.3.2 Section 17 - System Test and Verification Specification v.1.4.2**
for additional information.

The RCTab software is developed with the following tools, policies, and
practices to ensure robust software quality and reliability. RCTab
testing relates only to the function of the software and performs no
parts and materials testing as we produce only software and do not
design or manufacture hardware. Testing follows standards laid out in
the VVSG Volume 2:2.12.1 with regard to V1:8.5.

<table>
<colgroup>
<col style="width: 14%" />
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
<th>1.0.1</th>
<th>Updated for RCTab v1.3.1</th>
<th>Melissa Hall</th>
</tr>
<tr class="odd">
<th>04/27/2023</th>
<th>1.0.0</th>
<th>Development Environment Specification</th>
<th>Ryan Kirby</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

