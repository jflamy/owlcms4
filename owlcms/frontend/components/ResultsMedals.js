/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import { PolymerElement, html } from "@polymer/polymer/polymer-element.js";

class ResultsMedals extends PolymerElement {
    static get is() {
        return "resultsmedals-template";
    }

    static get template() {
        return html`
<link rel="stylesheet" type="text/css" href="local/styles/[[video]]colors[[autoversion]].css">
<link rel="stylesheet" type="text/css" href="local/styles/[[video]]resultsCustomization[[autoversion]].css">
<link rel="stylesheet" type="text/css" href="local/styles/[[video]]results[[autoversion]].css">

<div class$="wrapper medals [[teamWidthClass]] [[inactiveClass]]" style$="[[sizeOverride]];">
    <div style$="[[inactiveBlockStyle]]">
        <div class="competitionName">[[competitionName]]</div><br>
        <div class="nextGroup">[[t.WaitingNextGroup]]</div>
    </div>
    <div class="attemptBar" style$="[[normalHeaderDisplay]]; margin-bottom:1em;">
        <div class="athleteInfo" id="athleteInfoDiv">
            <div class="fullName ellipsis" id="fullNameDiv" inner-h-t-m-l="[[displayTitle]]"></div>
        </div>
    </div>
    <div class="video" style$="[[videoHeaderDisplay]]">
        <div class="eventlogo"></div>
        <div class="videoheader"><span class="groupName">[[competitionName]] &ndash; [[displayTitle]]</span></div>
        <div class="federationlogo"></div>
    </div>
    <template is="dom-if" if="[[medalCategories]]">
        <table class$="results medals [[noLiftRanks]]" style$="[[hiddenGridStyle]]; padding-top: 0.5em;">
            <template is="dom-repeat" id="result-table" items="[[medalCategories]]" as="mc">
                <tr class="head" style$="[[leadersDisplay]]">
                    <td style="grid-column: 1 / -1; justify-content: left; font-weight: bold"
                        inner-h-t-m-l="[[mc.categoryName]]">
                    </td>
                </tr>
                <tr>
                    <td class="headerSpacer" style="grid-column: 1 / -1; justify-content: left;"
                        inner-h-t-m-l="&nbsp;">
                    </td>
                </tr>
                <tr class="head" style="[[mc.showCatHeader]]">
                    <!-- [[t.x]] references the translation for key ScoreLeader.x in the translation4.csv file -->
                    <th class="groupCol" inner-h-t-m-l="[[t.Group]]"></th>
                    <th class="name" inner-h-t-m-l="[[t.Name]]"></th>
                    <th class="category" inner-h-t-m-l="[[t.Category]]"></th>
                    <th class="yob" inner-h-t-m-l="[[t.Birth]]"></th>
                    <th class="custom1" inner-h-t-m-l="[[t.Custom1]]"></th>
                    <th class="custom2" inner-h-t-m-l="[[t.Custom2]]"></th>
                    <th class="club" inner-h-t-m-l="[[t.Team]]"></th>
                	<th class="vspacer"></th>
                    <th style="grid-column: span 3;" inner-h-t-m-l="[[t.Snatch]]"></th>
                	<th class="best" inner-h-t-m-l="[[t.Best]]"></th>
                    <th class="rank" inner-h-t-m-l="[[t.Rank]]"></th>
                	<th class="vspacer"></th>
                    <th style="grid-column: span 3;" inner-h-t-m-l="[[t.Clean_and_Jerk]]"></th>
                	<th class="best" inner-h-t-m-l="[[t.Best]]"></th>
                    <th class="rank" inner-h-t-m-l="[[t.Rank]]"></th>
                	<th class="vspacer"></th>
                    <th class="total" inner-h-t-m-l="[[t.Total]]"></th>
                    <th class="totalRank" inner-h-t-m-l="[[t.Rank]]"></th>
                    <th class="sinclair" inner-h-t-m-l="[[t.Sinclair]]"></th>
                    <th class="sinclairRank" inner-h-t-m-l="[[t.Rank]]"></th>
                </tr>
                <!-- tr>
                    <td class="headerSpacer" style="grid-column: 1 / -1; justify-content: left;"
                        inner-h-t-m-l="&nbsp;">
                    </td>
                </tr -->
                <template is="dom-repeat" id="result-table" items="[[mc.leaders]]" as="l">
                    <tr class="athlete" style$="[[leadersDisplay]]">
                        <td class="groupCol">
                            <div>[[l.group]]</div>
                        </td>
                        <td class$="name [[l.classname]]">
                            <div class="ellipsis">[[l.fullName]]</div>
                        </td>
                        <td class="category">
                            <div>[[l.category]]</div>
                        </td>
                        <td class="yob">
                            <div>[[l.yearOfBirth]]</div>
                        </td>
                        <td class="custom1">
                            <div>[[l.custom1]]</div>
                        </td>
                        <td class="custom2">
                            <div>[[l.custom2]]</div>
                        </td>
                        <td class="club">
                            <div class="ellipsis">[[l.teamName]]</div>
                        </td>
                        <td class="vspacer"></td>
                        <template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
                            <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                <div>[[attempt.stringValue]]</div>
                            </td>
                        </template>
                        <td class="best">
                            <div inner-h-t-m-l="[[l.bestSnatch]]"></div>
                        </td>
                        <td class="rank">
                            <div inner-h-t-m-l="[[l.snatchRank]]"></div>
                        </td>
                        <td class="vspacer"></td>
                        <template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
                            <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                <div>[[attempt.stringValue]]</div>
                            </td>
                        </template>
                        <td class="best">
                            <div inner-h-t-m-l="[[l.bestCleanJerk]]"></div>
                        </td>
                        <td class="rank">
                            <div inner-h-t-m-l="[[l.cleanJerkRank]]"></div>
                        </td>
                        <td class="vspacer"></td>
                        <td class="total">
                            <div>[[l.total]]</div>
                        </td>
                        <td class="totalRank">
                            <div inner-h-t-m-l="[[l.totalRank]]"></div>
                        </td>
                        <td class="sinclair">
                            <div>[[l.sinclair]]</div>
                        </td>
                        <td class="sinclairRank">
                            <div>[[l.sinclairRank]]</div>
                        </td>
                    </tr>
                </template>
                <tr>
                    <td class="filler" style="grid-column: 1 / -1; line-height:100%">&nbsp;</td>
                </tr>
            </template>
        </table>
    </template>
</div>`;
    }

    ready() {
        console.debug("ready");
        super.ready();
        document.body.setAttribute("theme", "dark");
        this.$.fullNameDiv.style.visibility = "visible";
        this.$.fullNameDiv.style.display = "flex";
    }

    start() {
        this.$.timer.start();
    }

    reset() {
        console.debug("reset");
        this.$.fullNameDiv.style.visibility = "visible";
        this.$.fullNameDiv.style.display = "flex";
    }

    down() {
        console.debug("refereeDecision");
    }

    doBreak() {
        console.debug("break");
        this.$.fullNameDiv.style.visibility = "visible";
        this.$.fullNameDiv.style.display = "flex";
    }

    groupDone() {
        console.debug("done");
        this.$.fullNameDiv.style.visibility = "visible";
        this.$.fullNameDiv.style.display = "flex";
    }

    refereeDecision() {
        console.debug("refereeDecision");
        //this.$.groupDiv.style.visibility = "visible";
        this.$.decisionDiv.style.display = "flex";
        this.$.weightDiv.style.display = "flex";
        this.$.timerDiv.style.display = "flex";
        this.$.breakTimerDiv.style.display = "none";
    }

    _isEqualTo(title, string) {
        return title == string;
    }

    isElementOverflowing(element) {
        var overflowX = element.offsetWidth < element.scrollWidth,
            overflowY = element.offsetHeight < element.scrollHeight;
        console.warn("overflowX " + overflowX);
        return (overflowX || overflowY);
    }

    wrapContentsInMarquee(element) {
        var marquee = document.createElement('marquee'),
            contents = element.innerText;

        marquee.innerText = contents;
        element.innerHTML = '';
        element.appendChild(marquee);
    }

    marqueeIfTooBig() {
        var element = this.$.records;

        if (this.isElementOverflowing(element)) {
            this.wrapContentsInMarquee(element);
        }
    }

}

customElements.define(ResultsMedals.is, ResultsMedals);