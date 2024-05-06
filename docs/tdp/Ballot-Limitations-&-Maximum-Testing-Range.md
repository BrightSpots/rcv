# RCTab v1.3.2 Section 20 - Process Ranked Choice Voting Contest v.1.2.1

> **RCTab v1.3.2 Section 20 - Process Ranked Choice Voting Contest
> v.1.2.1**
>
> document is solely for use in the State of California. This document
> can be expanded or updated as is necessary or required. Where
> relevant, this document refers to specific sections and requirements
> of the California Voting System Standards. Any recommendations listed
> in this document should not supersede user jurisdiction procedures or
> other controlling governance entities.

## Overview

Ranked Choice Voting (RCV) is a voting method where the voter is able to
identify their candidate choices in order of preference. Although voting
the ballot is the same, there are many variations used in processing
ballot selections to determine the candidates that are elected. This
section will address the methods currently in use in the United States
which fall into two major categories, single-winner RCV and multi-winner
RCV. Single-winner RCV is also known as Instant Runoff Voting (IRV).
Multi-winner RCV is also known as Single Transferable Vote (STV). Both
use multi-round processing methods to determine the winner(s) and
require Cast Voter Records (CVRs) containing all voter choices for RCV
contests to be processed. These methods are most commonly used in
jurisdictions where current law requires a winning candidate to have a
majority of votes cast (i.e., 50% +1 in a single seat contest) to avoid
the expensive alternative of holding a separate primary or runoff
election.

The processing used in each round, if a winner has not been determined,
is as follows. The candidate with the lowest vote total on the round is
considered eliminated. Each CVR containing the eliminated candidate as
the current 1<sup>st</sup> choice is processed to substitute the next
highest ranked continuing candidate (one that has not been eliminated)
to replace the eliminated candidate. If there are no choices left on a
given ballot, that ballot is considered exhausted. New round totals are
tabulated and a determination of whether a candidate now has sufficient
votes to win is made. If not, the process is repeated round by round
until a winner is or winners are determined.

There are several variations of each of the IRV and STV methods. Most
are common but a couple are specific to STV. Common variances include:

-   Handling of an overvote choice during the round-by-round processing
    – Is the ballot considered exhausted or is the choice skipped and
    checked for a subsequent go forward candidate?

-   Handling of an omitted choice/ranking during the round-by-round
    processing – Is the choice skipped, does this cause the ballot to be
    exhausted or is the choice skipped once but two skipped rankings in
    succession exhaust the ballot?

-   Handling of a duplicate ranking (selecting the same candidate for
    more than one ranking) during the round-by-round processing – Is the
    duplicate skipped in the same way as an omitted ranking or is the
    ballot considered exhausted?

-   Handling of candidate elimination – Are all candidates who have no
    chance of

> winning concurrently eliminated in the first round only or in any
> round, or is only one candidate eliminated in any round and multiple
> elimination of candidates not used?

-   Handling of tabulated voted 1<sup>st</sup> choices – Is tabulation
    of voted 1<sup>st</sup> choices used to determine if a candidate(s)
    has sufficient votes to be elected (thus avoiding the use of

> an RCV algorithm) or is the algorithm always used and tabulation of
> marked first choices ignored? Even if used, does calculation of the
> threshold for election include all ballots cast (include over and
> under vote totals) or is it based on total selection.

The flow chart below indicates the processing flow for either single or
a multi-seat contest with each block in the flow described.

> <img src="../media/image42.jpg"
> style="width:5.46048in;height:7.04899in" />

## Description

1.  Marked first-choice tabulation used?

Some RCV rules require (St. Paul and Minneapolis) or conditionally allow
(Oakland) a shortcut that can elect candidates by looking only at marked
first choices. This can help avoid what may be a more logistically
involved process of collecting and examining CVRs to determine effective
first choices. If a tabulation of marked first choices elects sufficient
candidates, the full RCV tabulation process is not needed and
examination of CVRs might be avoided. Some other jurisdictions use this
shortcut as a matter of practice.

If a ballot has a validly marked first choice, that is also the ballot's
effective first choice. However, a ballot can have an effective first
choice but not a marked first choice if the first choice is left blank
or is not valid but there is a second or subsequent choice that is
validly marked. Detailed rules for what is counted as an effective first
choice can vary by jurisdiction. See the description for block 6 for
additional details.

The Yes path is taken if an initial tabulation of marked first choices
is used. The No path is taken if the CVRs are used to initially
determine the effective first-choice candidate votes.

1.  Determine candidate vote counts & \# of ballots by marked
    first-choices

Votes are tabulated by considering only marked first-choice ballot
selections and as if the contest were a vote-for-one contest. If the
first choice is not marked or is overvoted on a given ballot, there will
not be any contribution to candidate votes. The number of ballots used
as the base for the threshold calculation is also determined based on
jurisdiction-specific rules or practice. St. Paul uses the total number
of ballots with validly marked first choices that count for candidates.
Minneapolis uses the number of cast votes. The Oakland rule uses the
number of ballots cast except for those with a marked overvote.

1.  Determine threshold (T) to win and elect candidates

The threshold identifies how many votes a candidate must have in order
to be elected based by the tabulation of marked first choices. If a
contest is only electing one candidate, the threshold is typically a
majority of the threshold base so that a candidate would need more than
50% of the threshold base to be elected. The Minneapolis threshold
formula for any number of candidates to be elected is:

> T= 1 + B / (N+1) rounded down to the nearest whole number

where T is the threshold, B is the number of relevant ballots and N is
the number of candidates to

be elected. Thus, in a contest where only one candidate is to be
elected, the Minneapolis threshold is floor (1 + B/2) and a candidate
must have at least that many votes to be elected. In a

contest with 4 candidates to be elected, the Minneapolis threshold would
be floor(1 + B/5) and a candidate must have at least that many marked
first-choice votes to be elected, i.e., more than 20%, since the number
of marked first-choice votes will be a whole number. Once the threshold
and threshold criterion are established, any candidates satisfying that
criterion are considered elected for the purposes of this tabulation of
marked first choices.

1.  All seats filled?

Are sufficient candidates elected because their number of marked
first-choice votes is greater than (or equal to, for Minneapolis) the
threshold to fill all seats in the contest? The Yes path is taken if
there are. The No path is taken if one or more positions have not been
filled due to insufficient candidates obtaining the required number of
marked first-choice votes. In a single- winner contest, taking the Yes
path only requires that one candidate be elected. If the No path is
taken, a full RCV tabulation is conducted, without using the results of
the tabulation of marked first choices. In particular, candidates
elected by marked first choices are considered unelected continuing
candidates at the beginning of the full RCV tabulation. If the Yes path
is taken, the full RCV tabulation is not required.

1.  Stage all CVRs

A full RCV tabulation requires every CVR to be examined to determine
which if any candidate the CVR will count for in the first round. This
block stages all CVRs for that determination. All candidates begin the
first round as continuing candidates, neither elected nor eliminated,
without any votes.

1.  Process a staged CVR

A single CVR taken from a collection of staged CVRs is processed to
determine the candidate for whom the ballot will next count, the CVR’s
highest ranked continuing candidate, if such a candidate exists. A
continuing candidate is defined as a candidate that is neither
eliminated (a.k.a defeated) nor elected.

CVRs are staged for processing in this step from three sources: 1) in
block 5, all CVRs are staged for round 1, 2) in block 20 after a
candidate has been defeated, and 3) for multi-seat contests in block 18
after a candidate has been elected with surplus votes, the votes for a
candidate in excess of the threshold. Minneapolis rules, CA SB 1288, and
HR 3057 redistribute surplus as a fractional vote for all CVRs that
counted for the elected candidate. Cambridge rules distribute surplus as
a whole vote per CVR but only for a subset of CVRs. See the description
of

block 18 for further details. Typically, the CVR’s counting for a
candidate is staged for reassignment after that candidate is eliminated
or elected and before the next round’s vote counts are tallied, unless
it can be otherwise determined that a next round is not needed.

Determining the highest-ranked (most preferred) continuing candidate can
be fairly straightforward if the voter has simply ranked candidates in
order of preference. However, there are several irregular ranking
situations that a voter might mark and which are treated according to
jurisdiction-specific rules: 1) no candidate ranked at a ranking level,
2) ranking more than one candidate at a ranking level, and 3) ranking a
candidate at more than one ranking level. The following describes how
these situations are treated by various jurisdictions:

-   Unvoted choice (no candidate ranked at a ranking level)

    -   Ballot considered exhausted

    -   Ballot considered exhausted if there are two unvoted choices in
        succession

    -   Skipped and subsequent choice processed, if any

-   Overvoted choice (more than one candidate ranked at a ranking level)

    -   Ballot considered exhausted (most common)

    -   Not considered overvoted if doesn’t contain more than one
        continuing candidate (i.e., Takoma Park Md)

    -   Skipped and subsequent choice processed, if any

-   Repeated ranking of a previously ranked candidate

    -   Ballot considered exhausted

    -   Skipped and subsequent choice processed, if any

1.  Valid most preferred continuing candidate?

The No path is taken for the processed CVR when there is no valid
selection for the highest ranked continuing candidate. An invalid choice
could include selections for more than one continuing candidate
depending on jurisdiction rules. The Yes path is taken if there is a
valid selection for the highest ranked continuing candidate.

1.  Ballot is exhausted. Update reason stats for reporting

This CVR will not be included in any further tabulation processing as
there are no more validly ranked continuing candidates available. This
may be due to not containing further ranking selections, an overvote in
the current ranking choice preventing consideration of subsequent
rankings, the repeated selection of an already ranked candidate, or that
all subsequent ranking selections are for eliminated or already elected
candidates. The reason for the ballot being considered exhausted is
recorded for purposes of reporting round results.

1.  CVR no longer processed

Since the CVR/ballot does not contain a valid subsequent choice for the
highest ranked continuing candidate, that ballot is not subject to
further tabulation.

1.  Assign CVR to that candidate

The CVR is assigned to count for its highest-ranked (most preferred)
continuing candidate. The ballot will contribute one full vote or a
surplus fraction of a vote to that candidate’s vote total. A

surplus fraction of a vote can be used in a multi-seat contest using
rules from Minneapolis, SB 1288, or HR 3057.

1.  All CVRs processed?

The No path is taken to process the next staged CVR if some staged
ballots still remain to be processed. The Yes path is taken if all
staged ballots have been processed.

1.  Tabulate vote totals

The vote totals for each candidate and for any other reporting
categories are tallied.

If there was a tabulation of marked first choices in block 2 (a.k.a
round 0), the candidate vote totals for round 1 can be higher, but never
lower, than the candidate totals of marked first choices. A candidate
vote total can be higher if there are one or more ballots with no marked
first choice, or in some jurisdictions an invalidly marked first choice,
but there is a valid candidate selection for a subsequent choice.

1.  Threshold exists for current round?

The No path is typically taken every time for a single-seat contest as
the majority threshold will be calculated for each round. The No path is
typically taken only for the first round for multi-seat contests, so
that the same threshold applies to all elected candidates, regardless of
the round in which they are elected. The Yes path is typically only
taken for the second and subsequent rounds of a multi-seat contest which
reuses the first-round threshold. Typically, a threshold from a
tabulation of marked first choices will not be reused here, especially
if that tabulation used a different threshold base.

1.  Determine threshold from vote totals

The total votes counting for candidates in the round is typically used
as the threshold base, i.e., the number of ballots / votes used to
calculate the threshold. For single-seat contests, the threshold is
typically expressed in terms of a majority of that threshold base, i.e.,
more than 50%. For multi-seat contests, the threshold can be expressed
as:

T = B / (S + 1) + X

where T is the threshold, B is the threshold base, S is the number of
seats to be filled, and X is some small extra amount that, depending on
the specific rules, might be as small as zero but is not bigger than one
whole vote.

There are two approaches to determining the extra amount X and the
threshold criterion in order to ensure that it is mathematically
impossible to elect too many candidates:

-   X must be greater than zero, but reaching the threshold is
    sufficient to be elected

-   X can be zero, but the threshold must be exceeded in order to be
    elected

Minneapolis and Cambridge rules use the first, more traditional approach
while the more recent rules in SB 1288 and HR 3057 use the second
approach. For example, Minneapolis rules describe X as being the result
of adding one and ignoring any fractional value, i.e., rounding down to
the nearest whole number. SB 1288 rounds up to the fifth decimal place,
its precision for calculating fractional votes. In the diagram, the
threshold criterion is expressed in terms of the second approach with
the understanding that the greater-than-or-equal-to criterion of the
first approach can be substituted as appropriate.

1.  Any candidate votes &gt;T?

The Yes path is typically taken if the vote total for any (continuing)
candidate satisfies the threshold criterion, i.e., is greater than the
threshold. In a multi-seat election, it is possible for more than one
continuing candidate to exceed the threshold in a round. The No path is
taken if there are no continuing candidates with a vote total that
satisfies the threshold criterion.

Minneapolis has an exceptional rule that requires, subject to defined
conditions, that the No path to be taken in order to eliminate one or
more candidates, even if one or more continuing candidates has enough
votes to satisfy the threshold criterion. CA SB 1288 has a default
provision and HR 3057 requires that the No path be taken for single-seat
contests as long as there are three or more continuing candidates. This
can extend the tabulation to show a one- on-one comparison between the
two finalists without changing which candidate is elected. San Francisco
has adopted this option in practice.

1.  One seat w/rules requiring end w/2 candidates?

For a single seat contest, the sum of existing RCV users require that
the recursive process of candidate elimination and promotion of the
subsequent highest-ranking continuing candidate continue until only 2
candidates remain even if a candidate reaches the threshold to be
elected. The Yes path is taken if these rules apply. The No path is
taken if the contest is either a multi- seat contest or the conventional
rules are used for a single seat contest.

1.  2 candidates left?

This block is reached if the rules for single seat contests require the
RCV process to continue until 2 candidates are left. The Yes path is
taken if there are only 2 candidates left and the winner will be
declared. The No path is taken if there are more than 2 candidates left
and cause the RCV process to continue.

1.  Elect candidate(s)

One or more of the continuing candidates with a vote total that
satisfies the threshold criterion are elected. Depending on
jurisdiction-specific rules, if there is more than one such candidate,
all

them might be elected or only one might be selected for being elected in
this round, typically the candidate with the most such votes.
Jurisdiction-specific rules for resolving a tie for having the most
votes may apply. A candidate that satisfies the threshold criterion but
is not elected remains a continuing candidate and is still eligible to
receive transferred votes from other candidates.

1.  All seats filled?

The Yes path is taken if this is a single seat contest or if all
required candidates in a multi-seat contest have been elected,
indicating the process has been completed. The No path is taken if it is
a multi-seat contest and all seats have not been filled.

1.  Calculate surplus votes transfer formula

In a multi-seat contest when one or more candidates are elected in a
round, but all seats are not filled, CVRs containing excess votes for
the elected candidate are staged for further processing along with the
other CVRs for continuing candidates. This step determines the formula
for how these CVRs are staged.

There are two methods currently used for handling surplus votes (votes
for an elected candidate that are in excess of the threshold).
Minneapolis, CA SB 1288, and HR 3057 each select all CVRs for the
elected candidate but assign each CVR a transfer vote value that is a
fraction of a whole vote that corresponds to the prorated ballot’s share
of the elected candidate’s surplus. The fraction is equal to the ratio
of the elected candidate’s surplus votes for the round divided by the
elected candidate’s total votes for that round. Jurisdiction-specific
rules may specify the precision and any rounding (typically rounding
down) that are associated with this arithmetic operation.

In contrast, Cambridge processes ballots from precincts in a randomly
chosen order and selects every Nth ballot where N is the total votes for
the elected candidate divided by the excess votes (rounded) and
transfers the full vote of the selected CVRs to continuing candidates.

1.  Stage CVRs with surplus votes for winning candidates

All CVRs for continuing candidates are staged including CVRs containing
surplus votes for any candidate elected in this round according to the
formula developed in Step 20. In Minneapolis, all CVRs will be
transferred with a vote value fraction times the CVR’s previous
transferred vote value. Note that the previous transferred value might
be a fraction if it was a surplus from a candidate elected in a previous
round. Jurisdiction specific rules may specify the precision and any
rounding (typically rounding down) in this multiplication.

In Cambridge, the CVRs will be selected and staged at full vote value,
according to the formula determined in Step 20.

1.  \# continuing candidates = \# unfilled seats

The No path is taken if there are more continuing candidates than the
number of unfilled seats. The Yes path is taken if the number of
continuing candidates is equal to the current number of unfilled seats.

1.  Defeat candidates with fewest votes and stage their CVRs

A common approach is to eliminate (a.k.a. defeat) the candidate with the
fewest votes and then transfer that candidate’s votes to other
candidates. There can be jurisdiction-specific rules for how to resolve
a tie for having the fewest votes. Some jurisdictions run a lottery
while others look at previous round results and eliminate the candidate
with the lowest votes in the most recent previous round that is not
tied, using a lottery only if there is still a tie after looking at all
previous rounds.

It is also common for single-seat contests to allow or require a group
of candidates with the fewest votes to be eliminated in a single round
(a.k.a batch elimination) if their combined vote totals are less than
the candidate with the next higher vote total. Use of this option will
not change who is elected, i.e., its use is outcome invariant. San
Francisco and Oakland rules require use of this option but Alameda
County, which administers Oakland’s RCV elections, does not use it and
San Francisco has stopped using it in favor of other tabulation options
its voting system supports.

Some rules have exceptional elimination rules. For multi-seat contests,
Minneapolis and HR 3057 require certain candidate eliminations,
including batch eliminations, even though there might be continuing
candidates that satisfy the threshold criterion for being elected.
Cambridge requires elimination, after any surplus is transferred from
candidates elected in the first round of every candidate with fewer than
50 votes. The 50-vote minimum is derived from the requirement to have 50
signatures on a candidate’s nominating petition. For single-seat
contests, Minneapolis has a rule requiring elimination of a candidate
based on the total number ballots on which a

candidate is ranked. None of these exceptional elimination rules is
guaranteed to be outcome invariant compared to single elimination only
when there is no surplus to be transferred.

1.  Elect all continuing candidates

All continuing candidates are elected in order to fill the remaining
unfilled seats. This allows a candidate to be elected without satisfying
the threshold criterion.

1.  Done

Indicates that candidates have been elected to all positions to be
filled and the tabulation process is complete.

<table>
<colgroup>
<col style="width: 15%" />
<col style="width: 13%" />
<col style="width: 51%" />
<col style="width: 19%" />
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
<th>Updated to reflect v.1.3.1</th>
<th>Ryan Kirby</th>
</tr>
<tr class="odd">
<th>04/04/2023</th>
<th>1.2.0</th>
<th>Updated to Reflect RCTab v.1.3.0</th>
<th>Rene Rojas</th>
</tr>
<tr class="header">
<th>01/22/2022</th>
<th>1.1.0</th>
<th>Updated to Reflect RCTab and remove NYC</th>
<th>Rene Rojas</th>
</tr>
<tr class="odd">
<th>04/26/2021</th>
<th>1.0.0</th>
<th>Process Ranked Choice Voting Contest</th>
<th>Chris Hughes</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

