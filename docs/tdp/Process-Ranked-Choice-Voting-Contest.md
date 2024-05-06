# RCTab v1.3.2 Section 19 - Tabulation Options for RCV Tabulation v.1.2.1

> **RCTab v1.3.2 Section 19 - Tabulation Options for RCV Tabulation
> v.1.2.1**

document is solely for use in the State of California. This document can
be expanded or updated as is necessary or required. Where relevant, this
document refers to specific sections and requirements of the California
Voting System Standards. Any recommendations listed in this document
should not supersede user jurisdiction procedures or other controlling
governance entities.

Ranked choice voting is an umbrella term for a collection of voting
methods where voters rank candidates in order of preference and votes
are counted in rounds. Ranked choice voting elections occur in a series
of rounds. Those rounds can consist of counting votes, eliminating
candidates, transferring votes from eliminated candidates, electing
candidates, and transferring surplus votes from elected candidates.
Ranked choice voting is counted at both the granular ballot-by-ballot
level and at a contest-wide level. In other words, counting RCV requires
knowing details of how every individual ballot is marked as well as the
total number of votes for each candidate at each stage of the RCV count.

The following is an enumeration and discussion of the various tabulation
options that exist for Ranked Choice Voting (RCV) elections. Also
included is a glossary of ranked choice voting terms. This same glossary
is included in **<span class="mark">RCTab v1.3.2 Section 18 - User Guide
v.1.2.2</span>**.

Counting RCV elections proceeds as follows: all first-choice rankings
are counted first. If no candidate receives a specified number of votes
necessary for election (typically known as the threshold of election),
then the candidate with the fewest votes is eliminated. Ballots counting
for that candidate then transfer to the next-ranked candidate on each
ballot. These rounds of counting, elimination, and redistribution recur
until all seats up for election are filled. In some multi-winner
variations of RCV, candidates who receive more votes than necessary to
be elected – those candidates whose vote totals exceed the threshold of
election – are deemed to have surplus votes. If a candidate has a
surplus, the next step in counting is to transfer that surplus. Surplus
votes are transferred in various ways, but they follow the same logic as
elimination transfers: each ballot counting for a candidate with surplus
votes transfers vote value to the next-ranked candidate on that ballot.

There are many variations used in processing contest selections to
determine the candidates that are elected. Voting the ballot is the same
amongst the variants although the presentation of ballot design may
vary. This document addresses all variations currently in use in the
United States, which fall into two major categories: single-winner RCV
and variants of RCV electing multiple candidates. **Single-winner RCV**
is also known as Instant Runoff Voting (IRV). Variants of RCV that elect
multiple candidates include **multi-winner Ranked Choice Voting** (also
known as the Single Transferable Vote (STV) and Proportional ranked
choice voting), **Multi-Pass IRV** (also known as block-preferential
voting), **Bottoms-Up RCV**, and **Bottoms-Up RCV with Percentage
Threshold**. All use multi-round processing methods to determine the
outcome(s) and require the full set of ballots or cast vote records
(CVRs), for RCV contests to be processed. Purposes of this method
include avoiding holding a separate runoff election, eliminating the
effect of vote splitting or implementing proportional representation in
multi-member representative bodies.

The processing used in each single-winner RCV round, if there is no
apparent winner, is as follows. The candidate with the lowest vote total
in the round is considered eliminated. Each CVR containing the
eliminated candidate as the current 1st choice is processed to count for
the next highest-ranked continuing candidate (one that has not been
eliminated). If there are no choices left on a given ballot, that ballot
is considered exhausted and inactive for any subsequent rounds. The new
round tabulates totals and determines whether a candidate now has
sufficient votes to be elected. If not, the process is repeated
round-by-round until a candidate has sufficient votes to win.

In single-winner RCV, once a candidate has sufficient votes to be
elected (This typically means once a candidate has a majority of votes
in a round, but see item 1 for more detail.) tabulation will stop.

In bottoms-up RCV, bottoms-up RCV with percentage threshold, and
multi-pass IRV, tabulation proceeds in much the same way. Votes are
counted, candidates with the fewest votes are eliminated, and votes
transfer to the next candidate on each of those ballots. In bottoms-up
RCV, these rounds of counting recur until there are as many candidates
left as there are seats to fill. In bottoms-up RCV with percentage
threshold, these rounds of counting recur until all candidates remaining
have a share of votes equal to or greater than the percentage threshold.
In multi-pass IRV, counting proceeds slightly differently. Multi-Pass
IRV uses single-winner IRV for multi-winner contests by electing
candidates one at a time. The same process for counting rounds as
described above is followed. The count is then reset and run again, but
any rankings for an elected candidate are ignored. These passes of IRV
are run multiple times until all seats are filled. This method is not
designed to guarantee proportional representation, in contrast to STV
which was designed with the intent to provide proportional
representation.

In multi-winner ranked choice voting (aka single-transferable vote or
proportional RCV), rounds operate somewhat differently. Elimination
operates in the same way as all of the above: If no candidate receives a
previously specified number of votes necessary for election (typically
known as the threshold of election), then the candidate with the fewest
votes is eliminated. Ballots counting for that candidate then transfer
to the next-ranked candidate on each ballot. Where counting differs,
however, is when a candidate receives enough votes to win. If a
candidate receives more votes than necessary to be elected – if their
vote total exceeds the threshold of election – they are deemed to have
surplus votes. If a candidate has a surplus, the next step in counting
is to transfer that surplus. Surplus votes are transferred in various
ways, but they follow the same logic as elimination transfers: each
ballot counting for a candidate with surplus votes transfers vote value
to the next-ranked candidate on that ballot. In multi-winner RCV, rounds
of elimination and election recur until all seats up for election are
filled.

Note that in both multi-pass IRV and multi-winner RCV, once a candidate
is declared elected they cannot receive any more votes from vote
transfers.

There is variation in how each of the above tabulation methods actually
operate on the round-by-round and ballot-by-ballot level. Variations
related to how overvotes, repeated ranking, skipped rankings, candidate
elimination, surplus transfers, thresholds, ties and other factors are
handled. Each of these possible variations is described above. When a
variation applies to a specific type of RCV, that will be noted. Example
variations are briefly described in the bulleted list below:

-   Handling an overvoted choice during round-by-round processing – Is
    the ballot considered exhausted or is the choice skipped and the
    ballot checked for a subsequent continuing candidate?

-   Handling an omitted choice/ranking during round-by-round processing
    – Is the choice skipped? Does this cause the ballot to exhaust, are
    skipped rankings ignored or is one skipped ranking ignored but two
    skipped rankings in succession exhaust the ballot?

-   Handling of a repeat ranking (selecting the same candidate for more
    than one ranking) during the round-by-round processing – Is the
    repeated rank skipped in the same way as an omitted ranking or is
    the ballot considered exhausted?

-   Handling of candidate elimination – Are all candidates who have no
    mathematical chance of advancing concurrently eliminated in the
    first round only, in any round or is only one candidate eliminated
    in any round and multiple eliminations of candidates not used?

-   When to terminate tabulation – Is tabulation of voted 1st choices
    used to determine if a candidate(s) has sufficient votes to be
    elected (thus avoiding the use of an RCV algorithm) or is the
    algorithm always used? If used, does calculation of the threshold
    for election include all ballots cast (include over- and undervote
    totals) or is it based on total valid selections?

Most tabulation options are laid out explicitly in RCV laws, while some
laws cover only some of these options, and some of these options are
routinely excluded from RCV laws.

1.  Termination of tabulation (single-winner)

    1.  Declare winner if a candidate receives a majority of
        1<sup>st</sup> choice votes in the initial count. See (d) as
        well. (State of Maine, New York City)

        1.  If winner emerges from initial count, RCTab will not be
            used.

    2.  Declare winner when a candidate receives a majority of valid
        votes in any round (Basalt, Benton County, Berkeley, Las Cruces,
        Oakland, Payson, Portland, ME., San Leandro, Santa Fe, Takoma
        Park, MD., Vineyard)

        1.  Setting in RCTab: Winner Election Mode: Single-winner
            majority determines winner

    3.  Declare winner when a candidate receives a majority of all votes
        cast in contest, or most votes when two candidates remain
        (Minneapolis, St. Louis Park St. Paul, Minnesota jurisdictions)

        1.  Not included in RCTab

    4.  Declare a winner when only two candidates remain. (Eastpointe,
        State of Maine, New York City, San Francisco, CA.)

        1.  Setting in RCTab:

            1.  Winner Election Mode: Single-winner majority determines
                winner AND

            2.  Select “Continue until Two Candidates Remain”

> While any candidate achieving a majority of votes under (a), (b), or
> (c) will also be the winner under (d), the latter provides a more
> complete picture of the strength of support for the winner across all
> voters. While there is some incentive to choose option (a) if it saves
> on the cost of accumulating all cast vote records (CVRs) for RCV
> tabulation, if the CVRs are readily available, fully automated
> tabulation systems can go from (a) to (d) in seconds. San Francisco
> law requires (b) but the city opts to continue tabulation in line with
> (d). As with other options, (c) does not alter who will win an
> election but it does alter how contest results are presented. It may
> help to envision these different rules as different formulas. These
> numbers depend on the number of valid ballots in a round – how many
> ballots can be counted towards a candidate in the ranked choice voting
> contest in question.
>
> A majority is calculated in (b) jurisdictions using the following
> formula:
>
> T+B/(S+1)+1, disregarding fractions.
>
> T = Threshold
>
> B = Number of Continuing Ballots in the given round of counting
>
> S = Number of Seats to be Filled

A majority is calculated in (c) jurisdictions using the following
formula:

> T+B/(S+1)+1, disregarding fractions.
>
> T = Threshold
>
> B = Number of Valid Ballots Cast in the first round of counting
>
> S = Number of Seats to be Filled

1.  Determining which candidate(s) to eliminate (varies only in
    single-winner)

    1.  Lowest vote-getter (default in all methods)

        1.  Setting in RCTab: Don’t select “Use Batch Elimination”

    2.  Batch elimination (available only for single-winner contests)

        1.  Setting in RCTab: Select “Use Batch Elimination”

> There are two ways to eliminate candidates in single-winner RCV
> elections - one at a time or in batches. Eliminating candidates one at
> a time is straightforward - determine which candidate has the fewest
> votes and eliminate that candidate.
>
> Eliminating candidates in batches, using batch elimination, is
> somewhat more complicated. Eliminating candidates using batch
> elimination makes it faster to count a ranked choice voting election
> with many candidates - it is possible to have fewer rounds of counting
> if multiple candidates can be eliminated at once.
>
> Batch elimination eliminates all candidates who cannot receive enough
> votes to surpass the candidate with the next highest number of votes.
> Example: in a six candidate contest with 200 votes, Candidate A has 80
> votes, Candidate B has 70, and the other four combined have 50.
> Because those four candidates can never combine their votes to surpass
> Candidate B, they can be batch eliminated.
>
> Rule (eliminate the set of one or more candidates C1, C2, C3….Cn if
> sum of all votes for C1….Cn &lt; votes for candidate B and votes for B
> &lt; votes for candidate)
>
> If a specified candidate satisfies both of the following conditions,
> then all candidates with fewer votes may be designated as defeated:
>
> \(1\) At least one other candidate has at least as many votes as the
> specified candidate.
>
> \(2\) The specified candidate has more votes than the total votes for
> all candidates with fewer votes.
>
> In practice, when tabulating using RCTab or other computerized methods
> of counting, batch elimination has no effect on how long it takes to
> count the election. Batch elimination can reduce the number of rounds
> of counting required for an election, however. And if hand counting,
> batch elimination does provide a way to count an election more quickly
> if candidates can be batch eliminated.

1.  Overvotes (two or more candidates marked with the same ranking)

    1.  Skip to next highest-ranked candidate (Minneapolis, St. Louis
        Park, St. Paul)

        1.  Setting in RCTab: Overvote Rule: Always Skip to Next Rank

    2.  Exhaust ballot (Benton County, Berkeley, Eastpointe, Las Cruces,
        State of Maine, New York City, Oakland, Payson, Portland, San
        Francisco, Santa Fe, San Leandro, Telluride, Vineyard)

        1.  Setting in RCTab: Overvote Rule: Exhaust Immediately

    3.  Exhaust ballot unless only one overvoted candidate is a
        continuing candidate (Takoma Park, MD)

        1.  Setting in RCTab: Overvote Rule: Exhaust if multiple
            continuing

    4.  Suspend ballot until only one overvoted candidate is a
        continuing candidate then cast vote for that candidate or any
        subsequent highest-ranked continuing candidate. (Not currently
        used)

        1.  Not included in RCTab

> Options (a), (b), and (c) are in use in current RCV jurisdictions. The
> argument in favor of exhausting the ballot, option (b), is that this
> makes no judgements about voter intent. An error is an error and that
> terminates the ballot. While this is consistent with the treatment of
> overvotes in conventional plurality elections, RCV offers other valid
> options that keep a voter’s ballot alive.
>
> Skipping to the next highest-ranked candidate, option (a), is also
> used in a number of jurisdictions. This acknowledges the error but
> does not make it a terminal error. It simply treats the next
> highest-ranked choice as the first valid choice, which also avoids
> imposing any outside judgment about voter intent.
>
> Exhausting the ballot unless only one overvoted candidate is a
> continuing candidate, option (c), simply acknowledges that no overvote
> exists if only one of the marked candidates is a continuing candidate.
> This is because each round of tabulation only looks at votes for
> continuing candidates and ignores candidates that have been
> eliminated. There is, however, an inequity in this method in that it
> has unintended effects depending on the ranking at which the overvote
> occurs. No candidates are eliminated prior to the first round of
> tabulation, thus, any overvote at the first choice level would exhaust
> the ballot where an overvote at a later round might not exhaust the
> ballot.
>
> Option (d) is suggested as a means of eliminating the inequity of the
> different treatment of overvotes in different tabulation rounds. This
> option appears to maximize the opportunity for a voter’s ballot to be
> counted and counted as that voter intended. It should be noted,
> however, this option has not been and is currently not used in any RCV
> jurisdiction.

1.  Skipped rankings

    1.  Skip to next highest-ranked candidate (Berkeley, Las Cruces,
        Minneapolis, New York City, Oakland, St. Louis Park, St. Paul,
        San Francisco, Santa Fe, Telluride)

        1.  Setting in RCTab: Select “Unlimited” for “How Many
            Consecutive Skipped Ranks are Allowed”

    2.  Exhaust ballot (Colorado)

        1.  Setting in RCTab: Set “How Many Consecutive Skipped Ranks
            are Allowed” to 0

    3.  Exhaust ballot if two consecutive skipped rankings are
        encountered (Eastpointe, State of Maine, Takoma Park, MD., CA.
        Senate Bill 212, 2019)

        1.  Setting in RCTab: Set “How Many Consecutive Skipped Ranks
            are Allowed” to 1

> Skipping to the next highest-ranked continuing candidate without a
> limitation in consecutive skips, option (a), runs the risk of casting
> a ballot for a voter’s last choice candidate if they rank only their
> first and last choices and the first choice is eliminated.\*
>
> While exhausting a ballot after consecutive skipped rankings, option
> (c), minimizes this prospect of option (a), it does not entirely
> eliminate it (e.g., three candidates competing for one seat).
>
> Exhausting a ballot when a skipped ranking is encountered, option (b),
> again penalizes the voter in a draconian manner for what may be a
> simple oversight.
>
> Option (c) appears to minimize the potential adverse effects while
> maximizing the opportunity for the voter’s ballot to be counted as
> intended.

1.  Repeat rankings of the same candidate (sometimes referred to as
    duplicate ranking)

    1.  Ignore repeat rankings (Minneapolis, St. Paul, State of Maine,
        New York City, Oakland, San Leandro, Berkeley, Portland, ME.,
        Takoma Park, MD., Santa Fe)

        1.  Setting in RCTab: Don’t select “Exhaust on Multiple Ranks
            for the Same Candidate”

    2.  Exhaust ballot at repeat ranking (Eastpointe)

        1.  Setting in RCTab: Select “Exhaust on Multiple Ranks for the
            Same Candidate”

> Most jurisdictions are silent on this question in their RCV
> legislation. Information in this section is derived from practices
> followed in most jurisdictions. A standard tabulation algorithm will
> not detect a repeat ranking unless or until the candidate is
> eliminated. Once eliminated, the algorithm will simply look for the
> next highest-ranked continuing candidate and will pass over the repeat
> ranking as it would any other eliminated candidate.
>
> The practice of exhausting a ballot when a repeat ranking is
> encountered, option b, appears to emerge from a position that all
> voter “errors” should invalidate a ballot.

1.  Ties among last-place candidates

    1.  Decide by lot (Benton County, Berkeley, Eastpointe, State of
        Maine, Minneapolis, New York City, Oakland, Payson, Portland,
        ME., St. Louis Park, St. Paul, San Francisco, San Leandro, Santa
        Fe, Telluride, Vineyard, CA Senate Bill 212, 2019)

        1.  Setting in RCTab: Tiebreak Mode: Stop counting and ask

    2.  Most votes in previous round (Cambridge, Takoma Park, MD)

        1.  Setting in RCTab: Tiebreak Mode: Previous Round Counts then
            Stop counting and ask

    3.  Predetermine tiebreaking order and include in configuration or
        algorithm. (Allowed in State of Maine)

        1.  Setting in RCTab: Tiebreak Mode: Use candidate order in the
            config file or Generate config

> Last place ties pose a different kind of challenge in RCV elections
> than encountered in plurality elections. Here, time is of the essence
> since the continuation of the tabulation is dependent upon the
> resolution of a tie to determine which candidate is eliminated. Many
> states require that tied candidates be present for a tie resolution,
> something that is not desirable in RCV last-place ties where a tie
> must be broken before the tabulation can proceed.
>
> While “most votes in the previous round” generally is an easy,
> built-in tie-breaking mechanism, it suffers from one major drawback:
> it may violate the one-person-one-vote principle. This is due to the
> fact that it gives heavier weight to votes in the previous round than
> to votes in the current round in deciding who advances and who is
> eliminated. While this has not yet been litigated, it opens the door
> to that prospect.

1.  Last round ties/ties between candidates to elect

    1.  Defer to State law (Benton County, Berkeley, Maine, New York
        City, Oakland, San Francisco, San Leandro, Santa Fe, Telluride)

        1.  Setting in RCTab: Tiebreak Mode: Stop counting and ask

    2.  Decide by lot (Cambridge, Eastpointe, Minneapolis, St. Louis
        Park, St. Paul, Payson, Portland, ME., Santa Fe, Vineyard)

        1.  Setting in RCTab: Tiebreak Mode: Stop counting and ask

    3.  Most votes in previous round (Takoma Park, MD)

        1.  Setting in RCTab: Tiebreak Mode: Previous Round Counts then
            Stop counting and ask

    4.  Predetermine tiebreaking order and include in configuration or
        algorithm. (Allowed in State of Maine)

        1.  Setting in RCTab: Tiebreak Mode: Use candidate order in the
            config file or Generate config

> Last round ties in RCV elections are very similar to ties in plurality
> elections. Traditional tie breaking protocols may be employed if
> desired. In all jurisdictions that defer to State law, except New York
> City, state law requires the use of lots to decide ties. New York City
> law defers to party committees in primary elections and appears to be
> silent on how to decide ties in other circumstances. New York City has
> decided to decide these ties by lot.
>
> The option of using “most votes in the previous round” here is likely
> more problematic than in last-place tie situations in that a tie break
> for the win is more likely to be subject to litigation.

1.  RCV threshold calculation (multi-winner, bottoms-up RCV w/
    percentage threshold)

    1.  Multi-winner thresholds

        1.  Whole number threshold aka Droop Quota or most common
            threshold (Eastpointe, Minneapolis, Cambridge, MA.)

            1.  Setting in RCTab: Threshold Calculation Method: Compute
                using most common threshold formula

        2.  Fractional threshold aka HB (Hagenbach Bischoff) Quota (not
            used in US elections, but detailed in CA. Senate Bill 1288,
            2017, CA. Senate Bill 212, 2019)

            1.  Setting in RCTab: Threshold Calculation Method: Compute
                using HB Quota

        3.  Hare Quota (not used in official United States elections)

            1.  Setting in RCTab: Threshold Calculation Method: Compute
                using Hare Quota.

    2.  Bottoms-up RCV with percentage threshold (used in Alaska,
        Hawaii, Kansas, and Wyoming) 2020 Democratic Presidential
        Primaries

        1.  Percentage of votes cast in a round

            1.  (for discussion of this scroll to the bottom of this
                section)

        2.  Setting in RCTab: Percentage Threshold

> Multi-winner RCV uses a calculation based on the number of seats to be
> elected to determine what share of the votes cast in an election a
> candidate needs in order to win an election. That threshold is set by
> the formula floor(T=B/(S+1)+1) (for whole number thresholds),
> T&gt;B/(S-1)+n (for fractional thresholds), or floor(T=B/S) (for Hare
> Quota)
>
> T = Threshold
>
> B = Number of Valid Ballots Cast in election
>
> S = Number of Seats to be Filled
>
> S is set by Number of Winners in RCTab.
>
> n = a fraction greater than 0 and less than 1
>
> In percentages, this means the Droop or HB threshold in a three-seat
> election is 25%+ , a four-seat election is 20%+, a five-seat election
> is ~16.67%+, and so on. The Hare threshold/quota in a three-seat
> election is ~33.3% , a four-seat election is 25%, a five-seat election
> is 20%, and so on.
>
> While single-winner contests under options 1(a), 1(b), and 1(c)
> involve whole number thresholds, i.e., one vote more than a majority,
> multi-winner contests may use either whole number or fractional
> thresholds. This is due to the use of surplus transfers in
> multi-winner contests to credit a proportional share of extra votes a
> candidate receives (those above the threshold) to the next
> highest-ranked choice on ballots cast for a winner.
>
> Fractional thresholds provide for a higher degree of precision in
> deciding winners or eliminations. For instance, in a three-seat
> contest where 300 votes are cast, would the threshold be 101, 100.1,
> 100.01, 100.001, etc. votes? In most elections, such precision would
> make no difference, however, there are certainly cases where fractions
> of a vote could make the difference between winning, losing, or tying.
> This is particularly true in small elections or where there are
> numerous candidates, many of whom have weak support and the order of
> elimination is important.
>
> Some jurisdictions define the threshold as T&gt;B/(S+1) in which case
> the winner must receive a vote total greater than the threshold rather
> than greater than or equal to.

--

> Calculating the threshold in bottoms-up RCV with percentage threshold
> is accomplished by multiplying the number of continuing ballots in a
> round of counting by the percentage threshold set for that election.
>
> T = B\*P

T=Threshold

> B=Number of continuing ballots in a round
>
> P=Percentage threshold for this election.
>
> P is set by Percentage Threshold in RCTab
>
> For example, if the percentage threshold is 15% and 2000 ballots count
> in a round of counting, the threshold will be 300 votes.

1.  Multi-winner RCV surplus transfers

    1.  Fractional transfers (Eastpointe, MI, Minneapolis, CA.SB 1288,
        2017, CA. SB 212, 2019)

        1.  All multi-winner RCTab Winner Election Modes use fractional
            transfer.

    2.  Whole ballot transfer (Cambridge, MA.)

        1.  Not included in RCTab

> Candidates elected in multi-winner RCV elections frequently receive
> more votes than they need to win an election. These votes in excess of
> the threshold are called “surplus votes.”
>
> To ensure proportionality in multi-winner RCV elections, those surplus
> votes are transferred to later-ranked candidates on ballots counting
> for the candidate with a surplus. Only continuing candidates (those
> candidates not yet elected or eliminated) can receive surplus votes.
>
> There are two main methods of surplus transfer:
>
> 1\) Whole ballot transfers, where whole votes (each counting for one
> vote) transfer to a different candidate, instead of counting for the
> candidate with a surplus, until the candidate with a surplus has only
> as many votes as required by the threshold; or,
>
> 2\) Fractional transfers, where a fraction of every vote counting for
> a candidate with a surplus is transferred to later-ranked candidates
> on those ballots, until the candidate with a surplus has only as many
> votes as required by the threshold.
>
> Whole ballot transfers are currently used in Cambridge, MA elections.
> Whole ballot transfers are disfavored because they are not guaranteed
> to proportionally distribute a candidate’s surplus to the other
> candidates ranked on each voter’s ballot.
>
> Fractional transfers are currently used in Minneapolis, MN elections
> using a transfer method known as the “Weighted Inclusive Gregory
> Method” (WIGM). Here is a brief example showing how that transfer
> method works:
>
> Take a three-seat election with 100 votes cast. The threshold is 25+,
> from the formula T≥100/(3+1)+n.
>
> The Threshold is the number of valid ballots (B) divided by the number
> of seats to be filled (S), plus 1. n, as noted above, is some small
> amount between zero and one. In this case the threshold could be a
> fraction of a vote more than the 25 votes in our example.
>
> **How do we obtain a fraction of a vote?**
>
> First, Calculate a "Surplus Fraction." This is the candidate's extra
> votes (above the threshold) / the candidate's total votes.
>
> Surplus Fraction (SF) = Candidate's extra votes/Candidate's total
> votes
>
> e.g.: T = 25+, Candidate's total votes = 31
>
> SF = (31-25)/31 = 6/31 = .1935<sup>\[\*\]</sup>
>
> With a threshold of 25 votes and a candidate who received 31 votes,
> the surplus fraction would be 6/31 or .1935.
>
> **This becomes the "Transfer Value" to apply to each ballot's share of
> the surplus.**
>
> Next, each of the 31 voters' next highest-ranked choice receives .1935
> of a vote as the "transfer value" of the surplus.
>
> This "transfers" 5.9985 (31 x .1935) votes to other candidates,
> leaving the winning candidate with 25.0015 votes, which is &gt;25 or
> the Threshold.
>
> No preferences are wasted, the order of counting is not a factor and
> each voter's ballot shares equally in the surplus.
>
> If the last seat(s) in a multi-winner contest is filled and a vote
> surplus exists, this surplus can be redistributed, leaving all
> candidates with vote totals equal to the threshold. In general,
> neither the size of a vote surplus nor the order in which candidates
> are declared elected has any substantive meaning in multi-winner
> elections. What is important is which candidates achieve the threshold
> number of votes. In our example, 25.0001 would be the smallest number
> &gt; or = (≥) to 25, thus, all winners would receive 25.0001 votes and
> any remainder would be a “residual surplus” (not counting for any
> candidate) due to rounding.
>
> There are other methods of fractional surplus transfer, but none are
> used in the United States, so we do not cover them here. All
> international uses of multi-winner RCV (in Australia, Ireland, Malta,
> New Zealand, Northern Ireland, and Scotland) use fractional transfers.

1.  How many surpluses to transfer at once (multi-winner RCV)

    1.  Transfer one surplus at a time (Cambridge, Eastpointe,
        Minneapolis)

        1.  Setting in RCTab: Winner Election Mode: Multi-winner allow
            only one winner per round

    2.  Transfer all possible surpluses at once (CA SB 1288 2017, CA SB
        212 2019)

        1.  Setting in RCTab: Winner Election Mode: Multi-winner allow
            multiple winners per round

> It is possible for multiple candidates to cross the threshold of
> election in a single round in multi-winner ranked choice voting. If
> that occurs, there are two questions to answer: how many surpluses can
> transfer at once? And, if only one surplus can transfer, which one
> transfers?
>
> There are two options for transferring surpluses: transfer just one
> surplus or transfer all available surpluses. If just one surplus will
> transfer, then the largest surplus (the candidate with the most votes)
> transfers. If multiple surpluses will transfer, then all surplus votes
> transfer at once based on each individual winning candidate’s transfer
> value (as described in (9)). Votes transferring from winning
> candidates can transfer only to candidates still in the contest – if a
> winning candidate (a candidate transferring surplus or a candidate
> elected in a previous round) is ranked on a ballot, that ranking will
> be skipped in any surplus transfer. Eliminated candidates also cannot
> receive surplus votes.
>
> If, when transferring one surplus at a time, two or more candidates
> cross the threshold with a surplus and have surpluses of the same
> size, a tiebreaker will be needed to determine whose surplus transfers
> first. The tie-breaking protocols laid out in sections 6 and 7 are
> sufficient here.

1.  Miscellaneous rules/notes

    1.  Precinct Tabulation

        1.  Setting in RCTab: Tabulate by Precinct

    2.  Decimal places for vote counting

        1.  Setting in RCTab: Decimal places for vote counting

    3.  Number of rankings on a ballot

        1.  Setting in RCTab: Maximum number of candidates that can be
            ranked

> **Precinct Tabulation**
>
> Precinct-level results in ranked choice voting elections are similar
> to precinct results in plurality elections: they show how many votes
> each candidate got in a given precinct. RCV precinct-level results
> differ from plurality results in two important ways, however:

1.  They are calculated round by round;

2.  They depend on jurisdiction-wide results.

First: as with full results from a ranked choice voting election,
precinct-level ranked choice voting results must show the results of
each round of counting. Reporting precinct-level results can be done
using reporting methods similar to those used for the full RCV count,
with a spreadsheet breaking down each round of counting. Examples of
this are included later in this discussion. [<u>More detailed
visualizations</u>](https://www.urbanresearchmaps.org/nycrcv2021/) have
been produced as well.

Second: ranked choice voting is calculated on a jurisdiction-wide level.
In practice, that means precinct-level results in RCV provide somewhat
different information from precinct-level results in a plurality
election.

For example, in a plurality election with three candidates (Candidates
A, B, and C), 1000 votes are cast. Candidate A receives 400 votes,
Candidate B receives 350 votes, and Candidate C receives 250 votes. To
illustrate:

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Election Results</strong></th>
<th><strong>Vote Totals</strong></th>
</tr>
<tr class="odd">
<th><strong>Candidate A</strong></th>
<th>400</th>
</tr>
<tr class="header">
<th><strong>Candidate B</strong></th>
<th>350</th>
</tr>
<tr class="odd">
<th><strong>Candidate C</strong></th>
<th>250</th>
</tr>
<tr class="header">
<th><strong>Total Votes</strong></th>
<th>1000</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

Candidate A wins the election. Candidates A, B, and C may win different
precincts, however.

For example, there may be a precinct where Candidate C is very popular:

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Precinct 3 Results</strong></th>
<th><strong>Vote Totals</strong></th>
</tr>
<tr class="odd">
<th><strong>Candidate A</strong></th>
<th>20</th>
</tr>
<tr class="header">
<th><strong>Candidate B</strong></th>
<th>30</th>
</tr>
<tr class="odd">
<th><strong>Candidate C</strong></th>
<th>50</th>
</tr>
<tr class="header">
<th><strong>Total Votes in Precinct</strong></th>
<th>100</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

We generally say that candidate C lost the contest but won this
precinct.

Compare this with an RCV election with this same set of candidates (A,
B, and C). Again, 1000 votes are cast. Candidate A receives 400 first
choices, B receives 350 first choices, and C receives 250 first choices.
Because Candidate C has the fewest first choices, C is eliminated. Their
votes then transfer to the next candidate ranked on each ballot.
Candidate A receives 150 votes and Candidate B receives 100 votes.
Candidate A wins with 550 votes and Candidate B winds up with 450 votes.
To illustrate:

<table>
<colgroup>
<col style="width: 25%" />
<col style="width: 24%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Round-By-Round Results</strong></th>
<th><strong>Round 1</strong></th>
<th><strong>Transfer</strong></th>
<th><strong>Round 2</strong></th>
</tr>
<tr class="odd">
<th>Candidate A</th>
<th>400</th>
<th>+150</th>
<th>550</th>
</tr>
<tr class="header">
<th>Candidate B</th>
<th>350</th>
<th>+100</th>
<th>450</th>
</tr>
<tr class="odd">
<th>Candidate C</th>
<th>250</th>
<th>-250</th>
<th>0</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

There may be a precinct where Candidate C leads in first choices. In
this precinct, Candidate C may have 50 first choices, Candidate B may
have 30, and Candidate A may have 20. Candidate C may lead in the
precinct but, because C has the fewest votes in the contest as a whole,
they are eliminated, and their votes transfer.

<table>
<colgroup>
<col style="width: 26%" />
<col style="width: 23%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Precinct 3 Round-By-Round Results</strong></th>
<th><strong>Round 1</strong></th>
<th><strong>Transfer</strong></th>
<th><strong>Round 2</strong></th>
</tr>
<tr class="odd">
<th>Candidate A</th>
<th>20</th>
<th>+35</th>
<th>55</th>
</tr>
<tr class="header">
<th>Candidate B</th>
<th>30</th>
<th>+15</th>
<th>45</th>
</tr>
<tr class="odd">
<th>Candidate C</th>
<th>50</th>
<th>-50</th>
<th>0</th>
</tr>
<tr class="header">
<th>Total votes in Precinct</th>
<th>100</th>
<th></th>
<th>100</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

In ranked choice voting, Candidate C led this precinct in the first
round but, because they lost the contest, was eliminated and had their
votes transfer. Because they were eliminated and their votes
transferred, Candidate A wound up winning this precinct.

> **Decimal Places**
>
> The number of decimal places to use for voting counting is a detailed,
> granular decision relevant only to proportional RCV elections. Setting
> this makes it possible to hand count proportional RCV elections,
> because the fractions involved in multi-winner RCV can become unwieldy
> for a human to manage. All RCV jurisdictions in the US that have made
> this decision have set the number of decimal places at 4. It may be
> possible to remove the decimal place limitation on proportional RCV
> elections, but that would make those elections more challenging to
> replicate in a hand count.
>
> The rounding required to stick to the number of decimal places set for
> a given multi-winner RCV election can mean that the/a vote value is
> rounded away. That vote value is recorded as “residual votes” in
> multi-winner RCV elections in RCTab.
>
> **Rankings on the Ballot**
>
> The number of rankings on an RCV ballot varies across the United
> States. The most rankings permitted in an RCV election in the United
> States is 27 (the 2017 Cambridge City Council contest). Most RCV
> jurisdictions in the US allow voters to rank between 3 and 10
> candidates. These ranking limitations may have an impact on how many
> votes to exhaust - if there are many candidates in a contest and
> voters have a limited number of rankings, it is likely that more
> ballots will run out of candidates as more candidates are eliminated
> in the round-by-round count. More rankings likely means fewer
> exhausted ballots.

**Glossary of Terms**

<table>
<colgroup>
<col style="width: 22%" />
<col style="width: 77%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Batch elimination</strong></th>
<th><p>A simultaneous defeat of multiple continuing contest options for
which it is mathematically impossible to be elected or to prevail.</p>
<p><strong><mark>Discussion</mark></strong></p>
<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><mark>Rule (eliminate the set of one or more candidates C1, C2,
C3….Cn if sum of all votes for C1….Cn &lt; votes for candidate B and
votes for B &lt; votes for candidate)<br />
<br />
If a specified candidate satisfies both of the following conditions,
then all candidates with fewer votes may be designated as
defeated:<br />
(1) At least one other candidate has at least as many votes as the
specified candidate.<br />
(2) The specified candidate has more votes than the total votes for all
candidates with fewer votes.</mark></th>
</tr>
</thead>
<tbody>
</tbody>
</table></th>
</tr>
<tr class="odd">
<th><strong>Continuing Ballot</strong></th>
<th>A ballot that will be processed in the current contest RCV
round.</th>
</tr>
<tr class="header">
<th><strong>Continuing Contest Option</strong></th>
<th>A qualified candidate, measure, issue, or other contest option that
has not yet been elected, approved, or eliminated in the current round
or instance of RCV contest processing.</th>
</tr>
<tr class="odd">
<th><strong>Exhausted Ballot</strong></th>
<th><p>A ballot encountered in a round of RCV processing of a contest
that has no further valid rankings of continuing contest options or that
contains a condition in a subsequent choice that invalidates further
consideration of the ballot.</p>
<p>Equivalent term: Inactive Ballot</p>
<p><strong><mark>Discussion</mark></strong></p>
<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><p>The term ‘exhausted ballot’ is the term in use and in practice,
in legislation and other authoritative elections official and election
administrator documents including RFPs, RFIs, manuals, and documentation
as well as in use in the discourse in the field. Therefore, this term is
preserved to achieve the purpose, goals, and benefits of this
standard.</p>
<p>In Multi-Pass IRV all ballots, including exhausted ballots, are
revived when beginning the count to fill a second or later
seat.</p></th>
</tr>
</thead>
<tbody>
</tbody>
</table></th>
</tr>
<tr class="header">
<th><strong>Highest-ranked continuing contest option</strong></th>
<th>The next preferred continuing contest option on a given ranked
ballot.</th>
</tr>
<tr class="odd">
<th><strong>Highest Continuing Ranking</strong></th>
<th>The ranking on a voter's ballot with the lowest numerical value or
the next highest position in sequence for a continuing RCV contest
option.</th>
</tr>
<tr class="header">
<th><strong>Inactive Ballot</strong></th>
<th><p>Equivalent to <a
href="https://docs.google.com/document/d/18jMvqrsjuVTlkQIUg9oH0GTEYB_1ZOjt/edit#bookmark=kix.5evv11x7prj1"><u>Exhausted
Ballot</u></a>.</p>
<p><strong><mark>Discussion</mark></strong></p>
<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th>See also: <a
href="https://docs.google.com/document/d/18jMvqrsjuVTlkQIUg9oH0GTEYB_1ZOjt/edit#bookmark=kix.mlnv3k29hug8"><u>Discussion
for Exhausted Ballot</u></a>.<br />
The term “Exhausted Ballot” is used extensively as a term of art in the
RCV legislation, local jurisdiction artifacts, contracts, RFPs, and
therefore in the field. Therefore, for the purpose of NIST SP 1500-107
we preserve this as a key existing label for the same concept as the
newly and uniquely coined Inactive Ballot term.</th>
</tr>
</thead>
<tbody>
</tbody>
</table></th>
</tr>
<tr class="odd">
<th><strong>Majority of Votes</strong></th>
<th><p>Greater than 50-percent of the votes counted for a contest for
all continuing contest options in an RCV round.</p>
<p><strong><mark>Technical Definition</mark></strong></p>
<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th>majorityOfVotes = (sum(aggregate(votesContinuing)) /2) +1</th>
</tr>
</thead>
<tbody>
</tbody>
</table>
<p><strong><mark>Discussion</mark></strong></p>
<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><mark>“Majority of votes” shall mean fifty percent (50%) plus one of
the votes cast on continuing ballots. (San Leandro)</mark></th>
</tr>
</thead>
<tbody>
</tbody>
</table></th>
</tr>
<tr class="header">
<th><strong>Mathematically Impossible to be Elected or
Prevail</strong></th>
<th><p>A contest option in an RCV contest where the current vote total
plus all votes that could possibly be transferred to it in future rounds
would not be sufficient to surpass the contest option with the next
higher current vote total.</p>
<p><strong><mark>Discussion</mark></strong></p>
<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th>See <a
href="https://docs.google.com/document/d/18jMvqrsjuVTlkQIUg9oH0GTEYB_1ZOjt/edit#bookmark=kix.qhz8a3ro2mis"><u>Batch
Elimination</u></a> for operation.</th>
</tr>
</thead>
<tbody>
</tbody>
</table></th>
</tr>
<tr class="odd">
<th><strong>Overvote</strong></th>
<th><p>Occurs when the number of selections made by a voter in a contest
is more than the maximum number allowed.</p>
<p>Discussion</p>
<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th>As applied to RCV, overvotes may result in a skipped selection
rather than a lost vote. The consequences of the overvote depend on the
RCV rule being applied.</th>
</tr>
</thead>
<tbody>
</tbody>
</table></th>
</tr>
<tr class="header">
<th><strong>Overvote RCV ranking</strong></th>
<th>A ranking assigned to more than one contest option.</th>
</tr>
<tr class="odd">
<th><strong>Ranking, RCV</strong></th>
<th>The number or position selected for a contest option to indicate a
voter's ranked preference for that option.</th>
</tr>
<tr class="header">
<th><strong>Repeated ranking</strong></th>
<th><p>Selection of more than one ranking for the same contest option
for the contest being counted.</p>
<p><strong>Discussion</strong></p>
<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th>Sometimes referred to as Duplicate Ranking. Not to be confused with
“duplicate ranking” when that term is used to refer to an overvote.</th>
</tr>
</thead>
<tbody>
</tbody>
</table></th>
</tr>
<tr class="odd">
<th><strong>Residual surplus</strong></th>
<th>Any vote value lost due to rounding/truncation during the surplus
transfer process.</th>
</tr>
<tr class="header">
<th><strong>Round of counting or round</strong></th>
<th><p>A round is a subprocess in the tabulation process during which
current votes for all continuing contest options are counted in
accordance with the applicable tabulation method counting rules.</p>
<p><strong><mark>Technical Definition</mark></strong></p>
<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><p>A round is a subprocess in the tabulation counting process during
which current votes for all continuing contest options are counted in
accordance with the applicable tabulation method counting rules.</p>
<p>A round is for the purpose of determining the following:</p>
<ul>
<li><p>whether a contest option has achieved a majority or a
threshold,</p></li>
<li><p>whether and which contest option or contest options are to be
eliminated,</p></li>
<li><p>redistribution of surplus in a multi-winner contest.</p></li>
</ul></th>
</tr>
</thead>
<tbody>
</tbody>
</table>
<p><strong><mark>Discussion</mark></strong></p>
<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><p>A round is for the purpose of determining the following:</p>
<ul>
<li><p>whether a contest option has achieved a majority or a
threshold;</p></li>
<li><p>whether and which contest option or contest options are to be
eliminated; or</p></li>
<li><p>redistribution of surplus in a multi-winner contest.</p></li>
</ul></th>
</tr>
</thead>
<tbody>
</tbody>
</table></th>
</tr>
<tr class="odd">
<th><strong>Skipped ranking, RCV</strong></th>
<th>When a voter omits a ranking and ranks a contest option at a
subsequent ranking.</th>
</tr>
<tr class="header">
<th><strong>Surplus</strong></th>
<th><p>The number of votes cast for a contest option in excess of the
number required to meet or exceed the applicable threshold rule.</p>
<p><strong><mark>Discussion</mark></strong></p>
<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th>Applies to multi-winner RCV, aka STV</th>
</tr>
</thead>
<tbody>
</tbody>
</table></th>
</tr>
<tr class="odd">
<th><strong>Surplus fraction</strong></th>
<th><p>The proportion of each vote to be transferred when a surplus is
transferred.</p>
<p><strong>Technical Definition</strong></p>
<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th>The quotient, rounded down to n decimal places, of a contest
option’s surplus divided by the total number of votes for the contest
option in the round in which the surplus occurs.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>
<p><strong><mark>Discussion</mark></strong></p>
<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th>Applies to multi-winner RCV, aka STV</th>
</tr>
</thead>
<tbody>
</tbody>
</table></th>
</tr>
<tr class="header">
<th><strong>Threshold/Quota, RCV</strong></th>
<th>The number of votes that are sufficient for a contest option to
prevail. In STV, it is also the point at or above which additional votes
for a contest option are considered to be surplus.</th>
</tr>
<tr class="odd">
<th><strong>Transfer value</strong></th>
<th><p><mark>The transfer value of a ballot is the one vote or portion
of a vote that the ballot will contribute to the vote total for the
ballot’s highest-ranked continuing contest option after a surplus
transfer or elimination of one or more contest options.</mark></p>
<p><strong>Technical Definition</strong></p>
<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th>When used in surplus transfer, the product, rounded down to n
decimal places, of a ballot’s value multiplied by a contest option’s
surplus fraction.</th>
</tr>
</thead>
<tbody>
</tbody>
</table></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

<table>
<colgroup>
<col style="width: 15%" />
<col style="width: 13%" />
<col style="width: 53%" />
<col style="width: 17%" />
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
<th>Kelly Sechrist</th>
</tr>
<tr class="odd">
<th>04/04/2023</th>
<th>1.2.0</th>
<th>Updated to Reflect RCTab v.1.3.0</th>
<th>Rene Rojas</th>
</tr>
<tr class="header">
<th>01/19/2022</th>
<th>1.1.0</th>
<th>General Revisions and Edits for Clarity</th>
<th>Ryan Kirby</th>
</tr>
<tr class="odd">
<th>04/26/2021</th>
<th>1.0.0</th>
<th>Tabulation Options for RCV Tabulation</th>
<th>Chris Hughes</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

