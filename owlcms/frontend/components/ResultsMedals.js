/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
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
<link rel="stylesheet" type="text/css" href="local/styles/results.css">

<div class$="wrapper [[teamWidthClass]] [[inactiveClass]]">
    <div style$="[[inactiveBlockStyle]]">
        <div class="competitionName">[[competitionName]]</div><br>
        <div class="nextGroup">[[t.WaitingNextGroup]]</div>
    </div>
    <div class="attemptBar" style$="[[hiddenBlockStyle]]">
        <div class="athleteInfo" id="athleteInfoDiv">
            <div class="fullName ellipsis" id="fullNameDiv" inner-h-t-m-l="[[t.Medals]]"></div>
        </div>
    </div>
    <template is="dom-if" if="[[medalCategories]]">
        <table class="results" style$="[[hiddenGridStyle]]; padding-top: 1em;">
            <template is="dom-repeat" id="result-table" items="[[medalCategories]]" as="mc">
                <tr class="head" style="[[leadersDisplay]]">
                    <td style="grid-column: 1 / -1; justify-content: left; font-weight: bold"
                        inner-h-t-m-l="[[mc.categoryName]]">
                    </td>
                </tr>
                <tr class="head">
                    <!-- [[t.x]] references the translation for key ScoreLeader.x in the translation4.csv file -->
                    <th class="groupCol" inner-h-t-m-l="[[t.Group]]"></th>
                    <th class="name" inner-h-t-m-l="[[t.Name]]"></th>
                    <th class="category" inner-h-t-m-l="[[t.Category]]"></th>
                    <th class="narrow" inner-h-t-m-l="[[t.Birth]]"></th>
                    <th class="club" inner-h-t-m-l="[[t.Team]]"></th>
                    <th style="grid-column: span 3;" inner-h-t-m-l="[[t.Snatch]]"></th>
                    <th class="rank" inner-h-t-m-l="[[t.Rank]]"></th>
                    <th style="grid-column: span 3;" inner-h-t-m-l="[[t.Clean_and_Jerk]]"></th>
                    <th class="rank" inner-h-t-m-l="[[t.Rank]]"></th>
                    <th class="narrow" inner-h-t-m-l="[[t.Total]]"></th>
                    <th class="totalRank" inner-h-t-m-l="[[t.Rank]]"></th>
                </tr>
                <tr>
                    <td class="spacer" style="grid-column: 1 / -1; justify-content: left;"
                        inner-h-t-m-l="[[t.Leaders]] [[categoryName]]">
                    </td>
                </tr>
                <template is="dom-repeat" id="result-table" items="[[mc.leaders]]" as="l">
                    <tr class="athlete" style="[[leadersDisplay]]">
                        <td class="groupCol">
                            <div>[[l.group]]</div>
                        </td>
                        <td class$="name [[l.classname]]">
                            <div class="ellipsis">[[l.fullName]]</div>
                        </td>
                        <td class="category">
                            <div>[[l.category]]</div>
                        </td>
                        <td class="narrow">[[l.yearOfBirth]]</td>
                        <td class="club">
                            <div class="ellipsis">[[l.teamName]]</div>
                        </td>
                        <template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
                            <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                <div>[[attempt.stringValue]]</div>
                            </td>
                        </template>
                        <td class="rank">
                            <div inner-h-t-m-l="[[l.snatchRank]]"></div>
                        </td>
                        <template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
                            <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                <div>[[attempt.stringValue]]</div>
                            </td>
                        </template>
                        <td class="rank">
                            <div inner-h-t-m-l="[[l.cleanJerkRank]]"></div>
                        </td>
                        <td class="narrow">
                            <div>[[l.total]]</div>
                        </td>
                        <td class="totalRank">
                            <div inner-h-t-m-l="[[l.totalRank]]"></div>
                        </td>
                    </tr>
                </template>
                <tr>
                    <td class="filler" style="grid-column: 1 / -1; line-height:50%">&nbsp;</td>
                </tr>
            </template>
        </table>
    </template>
</div>`;
    }

    ready() {
        console.debug("ready");
        super.ready();
        this.$.groupDiv.style.visibility = "visible";
        this.$.fullNameDiv.style.visibility = "visible";
        this.$.fullNameDiv.style.display = "flex";
        this.$.startNumberDiv.style.display = "flex";
        this.$.teamNameDiv.style.display = "flex";
        this.$.attemptDiv.style.display = "flex";
        this.$.weightDiv.style.display = "flex";
        this.$.timerDiv.style.display = "flex";
        this.$.breakTimerDiv.style.display = "none";
        this.$.decisionDiv.style.display = "none";
    }

    start() {
        this.$.timer.start();
    }

    reset() {
        console.debug("reset");
        //this.marqueeIfTooBig();
        this.$.timer.reset(this.$.timer);
        this.$.groupDiv.style.visibility = "visible";
        this.$.fullNameDiv.style.visibility = "visible";
        this.$.fullNameDiv.style.display = "flex";
        this.$.startNumberDiv.style.display = "flex";
        this.$.teamNameDiv.style.display = "flex";
        this.$.attemptDiv.style.display = "flex";
        this.$.weightDiv.style.display = "flex";
        this.$.timerDiv.style.display = "flex";
        this.$.breakTimerDiv.style.display = "none";
        this.$.decisionDiv.style.display = "none";
    }

    down() {
        console.debug("refereeDecision");
        this.$.groupDiv.style.visibility = "visible";
        this.$.startNumberDiv.style.display = "flex";
        this.$.teamNameDiv.style.display = "flex";
        this.$.attemptDiv.style.display = "flex";
        this.$.weightDiv.style.display = "flex";
        this.$.timerDiv.style.display = "flex";
        this.$.breakTimerDiv.style.display = "none";
        this.$.decisionDiv.style.display = "flex";
    }

    doBreak() {
        console.debug("break");
        this.$.groupDiv.style.visibility = "hidden";
        this.$.fullNameDiv.style.visibility = "visible";
        this.$.fullNameDiv.style.display = "flex";
        this.$.startNumberDiv.style.display = "none";
        this.$.teamNameDiv.style.display = "none";
        this.$.attemptDiv.style.display = "none";
        this.$.weightDiv.style.display = "none";
        this.$.timerDiv.style.display = "none";
        this.$.breakTimerDiv.style.display = "flex";
        this.$.decisionDiv.style.display = "none";
    }

    groupDone() {
        console.debug("done");
        this.$.groupDiv.style.visibility = "hidden";
        this.$.fullNameDiv.style.visibility = "visible";
        this.$.fullNameDiv.style.display = "flex";
        this.$.startNumberDiv.style.display = "none";
        this.$.teamNameDiv.style.display = "none";
        this.$.attemptDiv.style.display = "none";
        this.$.weightDiv.style.display = "none";
        this.$.timerDiv.style.display = "none";
        this.$.breakTimerDiv.style.display = "none";
        this.$.decisionDiv.style.display = "none";
    }

    refereeDecision() {
        console.debug("refereeDecision");
        this.$.groupDiv.style.visibility = "visible";
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