/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import { PolymerElement, html } from "@polymer/polymer/polymer-element.js";

class Medals extends PolymerElement {
    static get is() {
        return "medals-template";
    }

    static get template() {
        return html`
    <link rel="stylesheet" type="text/css" href="local/styles/[[video]]colors[[autoversion]].css">
    <link rel="stylesheet" type="text/css" href="local/styles/scoreboard[[autoversion]].css">
    <div class$="wrapper [[teamWidthClass]] [[inactiveClass]]">
        <div style$="[[inactiveStyle]]">
            <div class="competitionName">[[competitionName]]</div><br>
            <div class="nextGroup">[[t.WaitingNextGroup]]</div>
        </div>
        <div class="attemptBar" style$="[[hiddenStyle]]">
            <div class="athleteInfo" id="athleteInfoDiv">
                <div class="fullName ellipsis" id="fullNameDiv" inner-h-t-m-l="[[t.Medals]]"></div>
            </div>
        </div>
        <!-- div class="group" style$="[[hiddenStyle]]">
            <div id="groupDiv">
                <span class="groupName">[[groupName]]</span>
            </div>
        </div -->
        <template is="dom-if" if="[[medalCategories]]">
            <template is="dom-repeat" id="result-table" items="[[medalCategories]]" as="mc">
                <div id="leaders" style$="padding-top:2em;[[hiddenStyle]] ">
                    <table class="results" id="leaders-table" style$="[[hiddenStyle]]">
                        <thead>
                            <tr>
                                <td colspan="100%" inner-h-t-m-l="[[mc.categoryName]]"></td>
                            </tr>
                        </thead>
                        <tr style="visibility:visible">
                            <!--  [[t.x]] references the translation for key Medals.x in the translation4.csv file -->
                            <th class="groupCol" inner-h-t-m-l="[[t.Group]]"></th>
                            <th class="name" inner-h-t-m-l="[[t.Name]]"></th><!-- kludge to have preformatted html -->
                            <th class="category" inner-h-t-m-l="[[t.Category]]"></th>
                            <th class="narrow" inner-h-t-m-l="[[t.Birth]]"></th>
                            <th class="club" inner-h-t-m-l="[[t.Team]]"></th>
                            <th colspan="3" inner-h-t-m-l="[[t.Snatch]]"></th>
                            <th class="showThRank" inner-h-t-m-l="[[t.Rank]]"></th>
                            <th colspan="3" inner-h-t-m-l="[[t.Clean_and_Jerk]]"></th>
                            <th class="showThRank" inner-h-t-m-l="[[t.Rank]]"></th>
                            <th class="narrow" inner-h-t-m-l="[[t.Total]]"></th>
                            <th class="thRank" inner-h-t-m-l="[[t.Rank]]"></th>
                        </tr>
                        <template is="dom-repeat" id="result-table" items="[[mc.leaders]]" as="l">
                            <template is="dom-if" if="[[l.isSpacer]]">
                                <tr>
                                    <td colspan="100%" style="height:0.1ex; border:none" class="spacer"></td>
                                </tr>
                            </template>
                            <template is="dom-if" if="[[!l.isSpacer]]">
                                <tr>
                                    <td class="groupCol">
                                        <div>[[l.group]]</div>
                                    </td>
                                    <td class$="name [[l.classname]]">
                                        <div>[[l.fullName]]</div>
                                    </td>
                                    <td class="category">
                                        <div>[[l.category]]</div>
                                    </td>
                                    <td class="narrow">[[l.yearOfBirth]]</td>
                                    <td class="club">
                                        <div>[[l.teamName]]</div>
                                    </td>
                                    <template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]"
                                        as="attempt">
                                        <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                            <div>[[attempt.stringValue]]</div>
                                        </td>
                                    </template>
                                    <td class="showRank">
                                        <div>[[l.snatchRank]]</div>
                                    </td>
                                    <template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]"
                                        as="attempt">
                                        <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                            <div>[[attempt.stringValue]]</div>
                                        </td>
                                    </template>
                                    <td class="showRank">
                                        <div>[[l.cleanJerkRank]]</div>
                                    </td>
                                    <td class="narrow">
                                        <div>[[l.total]]</div>
                                    </td>
                                    <td class="thRank">
                                        <div>[[l.totalRank]]</div>
                                    </td>
                                </tr>
                            </template>
                        </template>
                    </table>
                </div>
            </template>
        </template>
        <template is="dom-if" if="[[noCategories]]">
            <div>[[t.NoMedalCategories]]</div>
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

customElements.define(Medals.is, Medals);