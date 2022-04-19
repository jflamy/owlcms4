/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import { PolymerElement, html } from "@polymer/polymer/polymer-element.js";

class Results extends PolymerElement {
    static get is() {
        return "results-template";
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
                <div class="startNumber" id="startNumberDiv">
                    <span>[[startNumber]]</span>
                </div>
                <div class="fullName ellipsis" id="fullNameDiv" inner-h-t-m-l="[[fullName]]"></div>
                <div class="clubName ellipsis" id="teamNameDiv">
                    [[teamName]]
                </div>
                <div class="attempt" id="attemptDiv">
                    <span inner-h-t-m-l="[[attempt]]"></span>
                </div>
                <div class="weight" id="weightDiv">
                    [[weight]]<span style="font-size: 75%">[[t.KgSymbol]]</span>
                </div>
                <div class="timer athleteTimer" id="timerDiv">
                    <timer-element id="timer"></timer-element>
                </div>
                <div class="timer breakTime" id="breakTimerDiv">
                    <timer-element id="breakTimer"></timer-element>
                </div>
                <div class="decisionBox" id="decisionDiv">
                    <decision-element style="width: 100%" id="decisions"></decision-element>
                </div>
            </div>
        </div>
        <div class="group" style$="[[hiddenBlockStyle]]">
            <div id="groupDiv">
                <span class="groupName">[[groupName]]</span> &ndash; [[liftsDone]]
            </div>
        </div>
    
        <table class="results" style$="[[hiddenGridStyle]]; --top: 5; --bottom: 2">
            <template is="dom-if" if="[[athletes]]">
                <tr class="head">
                    <!-- [[t.x]] references the translation for key ScoreLeader.x in the translation4.csv file -->
                    <th class="groupCol" inner-h-t-m-l="[[t.Start]]"></th>
                    <th class="name" inner-h-t-m-l="[[t.Name]]"></th>
                    <th class="category" inner-h-t-m-l="[[t.Category]]"></th>
                    <th class="narrow" inner-h-t-m-l="[[t.Birth]]"></th>
                    <th class="club" inner-h-t-m-l="[[t.Team]]"></th>
                    <th style="grid-column: span 3;" inner-h-t-m-l="[[t.Snatch]]"></th>
                    <th class="showThRank" inner-h-t-m-l="[[t.Rank]]"></th>
                    <th style="grid-column: span 3;" inner-h-t-m-l="[[t.Clean_and_Jerk]]"></th>
                    <th class="showThRank" inner-h-t-m-l="[[t.Rank]]"></th>
                    <th class="narrow" inner-h-t-m-l="[[t.Total]]"></th>
                    <th class="thRank" inner-h-t-m-l="[[t.Rank]]"></th>
                </tr>
                <template is="dom-repeat" id="result-table" items="[[athletes]]" as="l">
                    <template is="dom-if" if="[[l.isSpacer]]">
                        <tr>
                            <td class="spacer">&nbsp;</td>
                        </tr>
                    </template>
                    <template is="dom-if" if="[[!l.isSpacer]]">
                        <tr class="athlete">
                            <td class$="groupCol [[l.classname]]">
                                <div class$="[[l.classname]]">[[l.startNumber]]</div>
                            </td>
                            <td class$="name [[l.classname]]">
                                <div class$="name [[l.classname]]">[[l.fullName]]</div>
                            </td>
                            <td class="category">
                                <div>[[l.category]]</div>
                            </td>
                            <td class="narrow">
                                <div>[[l.yearOfBirth]]</div>
                            </td>
                            <td class="club">
                                <div>[[l.teamName]]</div>
                            </td>
                            <template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
                                <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                    <div class$="[[attempt.goodBadClassName]] [[attempt.className]]">[[attempt.stringValue]]
                                    </div>
                                </td>
                            </template>
                            <td class="showRank">
                                <div>[[l.snatchRank]]</div>
                            </td>
                            <template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
                                <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                    <div class$="[[attempt.goodBadClassName]] [[attempt.className]]">[[attempt.stringValue]]
                                    </div>
                                </td>
                            </template>
                            <td class="showRank">
                                <div>[[l.cleanJerkRank]]</div>
                            </td>
                            <td class="narrow">[[l.total]]</td>
                            <td class="thRank">
                                <div>[[l.totalRank]]</div>
                            </td>
                        </tr>
                    </template>
                </template>
            </template>
            <template is="dom-if" if="[[leaders]]">
                <tr>
                    <td class="filler" style="grid-column: 1 / -1">&nbsp;</td>
                </tr>
                <tr class="head">
                    <td class="groupCol" inner-h-t-m-l="[[t.Group]]"></td>
                    <td class="name" inner-h-t-m-l="[[t.Name]]"></td>
                    <td class="category" inner-h-t-m-l="[[t.Category]]"></td>
                    <td class="narrow" inner-h-t-m-l="[[t.Birth]]"></td>
                    <td class="club" inner-h-t-m-l="[[t.Team]]"></td>
                    <td style="grid-column: span 3;" inner-h-t-m-l="[[t.Snatch]]"></td>
                    <td class="showThRank" inner-h-t-m-l="[[t.Rank]]"></td>
                    <td style="grid-column: span 3;" inner-h-t-m-l="[[t.Clean_and_Jerk]]"></td>
                    <td class="showThRank" inner-h-t-m-l="[[t.Rank]]"></td>
                    <td class="narrow" inner-h-t-m-l="[[t.Total]]"></td>
                    <td class="thRank" inner-h-t-m-l="[[t.Rank]]"></td>
                </tr>
                <!-- tr>
                                <td colspan="100%" inner-h-t-m-l="[[t.Leaders]] [[categoryName]]"></td>
                            </tr -->
                <template is="dom-repeat" id="result-table" items="[[leaders]]" as="l">
                    <!-- template is="dom-if" if="[[l.isSpacer]]">
                                                <tr>
                                                    <td colspan="100%" style="height:0.1ex; border:none" class="spacer"></td>
                                                </tr>
                                            </template -->
                    <template is="dom-if" if="[[!l.isSpacer]]">
                        <tr class="athlete">
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
                            <template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
                                <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                    <div>[[attempt.stringValue]]</div>
                                </td>
                            </template>
                            <td class="showRank">
                                <div>[[l.snatchRank]]</div>
                            </td>
                            <template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
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
            </template>
        </table>
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

customElements.define(Results.is, Results);