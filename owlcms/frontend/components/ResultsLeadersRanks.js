/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import { PolymerElement, html } from "@polymer/polymer/polymer-element.js";

class ResultsFull extends PolymerElement {
    static get is() {
        return "resultsfull-template";
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

    <table class="results"
        style$="[[hiddenGridStyle]]; --top: calc([[resultLines]] + 1); --bottom: [[leaderLines]]; --nbRanks: [[nbRanks]]">
        <template is="dom-if" if="[[athletes]]">
            <tr class="head">
                <!-- first row is all the row spans and the top cells -->
                <th class="groupCol" style="grid-row: span 2;">
                    <div style="display: grid; align-self: center" inner-h-t-m-l="[[t.Start]]"></div>
                </th>
                <th class="name" style="grid-row: span 2" inner-h-t-m-l="[[t.Name]]"></th>
                <th class="category" style="grid-row: span 2" inner-h-t-m-l="[[t.Category]]"></th>
                <th class="narrow" style="grid-row: span 2" inner-h-t-m-l="[[t.Birth]]"></th>
                <th class="club" style="grid-row: span 2" inner-h-t-m-l="[[t.Team]]"></th>
                <th style="grid-column: span calc(3 + var(--nbRanks));" inner-h-t-m-l="[[t.Snatch]]"></th>
                <th style="grid-column: span calc(3 + var(--nbRanks));" inner-h-t-m-l="[[t.Clean_and_Jerk]]"></th>
                <th style="grid-column: span calc(1 + var(--nbRanks));" inner-h-t-m-l="[[t.Total]]"></th>
            </tr>
            <tr class="head">
                <!-- second row is already partially filled from the row spans, only provide the empty cells
                    on the bottom row -->

                <th class="narrow">1</th>
                <th class="narrow">2</th>
                <th class="narrow">3</th>
                <template is="dom-repeat" id="snatchAgeGroups" items="[[ageGroups]]" as="sag">
                    <th>[[sag]]</th>
                </template>

                <th class="narrow">1</th>
                <th class="narrow">2</th>
                <th class="narrow">3</th>
                <template is="dom-repeat" id="cjAgeGroups" items="[[ageGroups]]" as="cjag">
                    <th>[[cjag]]</th>
                </template>

                <th class="narrow" inner-h-t-m-l="[[t.Total]]"></th>
                <template is="dom-repeat" id="totalAgeGroups" items="[[ageGroups]]" as="tag">
                    <th>[[tag]]</th>
                </template>

            </tr>
            <template is="dom-repeat" id="result-table" items="[[athletes]]" as="l">
                <template is="dom-if" if="[[l.isSpacer]]">
                    <tr>
                        <td class="spacer" style="grid-column: 1 / -1; justify-content: left;"
                            inner-h-t-m-l="[[t.Leaders]] [[categoryName]]">
                        </td>
                    </tr>
                </template>
                <template is="dom-if" if="[[!l.isSpacer]]">
                    <tr class="athlete">
                        <td class$="groupCol [[l.classname]]">
                            <div class$="[[l.classname]]">[[l.startNumber]]</div>
                        </td>
                        <td class$="name [[l.classname]]">
                            <div class$="name ellipsis [[l.classname]]">[[l.fullName]]</div>
                        </td>
                        <td class="category">
                            <div>[[l.category]]</div>
                        </td>
                        <td class="yob">
                            <div>[[l.yearOfBirth]]</div>
                        </td>
                        <td class="club">
                            <div class="ellipsis">[[l.teamName]]</div>
                        </td>
                        <template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
                            <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                <div class$="[[attempt.goodBadClassName]] [[attempt.className]]">[[attempt.stringValue]]
                                </div>
                            </td>
                        </template>
                        <template is="dom-repeat" id="snatchRanks" items="[[l.snatchRanks]]" as="sr">
                            <td class="rank">
                                <div inner-h-t-m-l="[[sr]]"></div>
                            </td>
                        </template>
                        <template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
                            <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                <div class$="[[attempt.goodBadClassName]] [[attempt.className]]">[[attempt.stringValue]]
                                </div>
                            </td>
                        </template>
                        <template is="dom-repeat" id="cleanJerkRanks" items="[[l.cleanJerkRanks]]" as="cjr">
                            <td class="rank">
                                <div inner-h-t-m-l="[[cjr]]"></div>
                            </td>
                        </template>
                        <td class="narrow">[[l.total]]</td>
                        <template is="dom-repeat" id="totalRanks" items="[[l.totalRanks]]" as="tr">
                            <td class="totalRank">
                                <div inner-h-t-m-l="[[tr]]"></div>
                            </td>
                        </template>
                    </tr>
                </template>
            </template>
        </template>
        <tr>
            <td class="filler" style="grid-column: 1 / -1">&nbsp;</td>
        </tr>
        <template is="dom-if" if="[[leaders]]">
            <tr class="head" style="[[leadersDisplay]]">
                <td style="grid-column: 1 / -1; justify-content: left;" inner-h-t-m-l="[[t.Leaders]] [[categoryName]]">
                </td>
            </tr>
            <tr>
            <tr style="[[leadersDisplay]]">
                <td class="spacer" style="grid-column: 1 / -1; justify-content: left;"
                    inner-h-t-m-l="[[t.Leaders]] [[categoryName]]">
                </td>
            </tr>
            <template is="dom-repeat" id="result-table" items="[[leaders]]" as="l">
                <template is="dom-if" if="[[!l.isSpacer]]">
                    <tr class="athlete">
                        <td class$="groupCol [[l.classname]]">
                            <div class$="[[l.classname]]">[[l.startNumber]]</div>
                        </td>
                        <td class$="name [[l.classname]]">
                            <div class$="name ellipsis [[l.classname]]">[[l.fullName]]</div>
                        </td>
                        <td class="category">
                            <div>[[l.category]]</div>
                        </td>
                        <td class="yob">
                            <div>[[l.yearOfBirth]]</div>
                        </td>
                        <td class="club">
                            <div class="ellipsis">[[l.teamName]]</div>
                        </td>
                        <template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
                            <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                <div class$="[[attempt.goodBadClassName]] [[attempt.className]]">[[attempt.stringValue]]
                                </div>
                            </td>
                        </template>
                        <template is="dom-repeat" id="snatchRanks" items="[[l.snatchRanks]]" as="sr">
                            <td class="rank">
                                <div inner-h-t-m-l="[[sr]]"></div>
                            </td>
                        </template>
                        <template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
                            <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                <div class$="[[attempt.goodBadClassName]] [[attempt.className]]">[[attempt.stringValue]]
                                </div>
                            </td>
                        </template>
                        <template is="dom-repeat" id="cleanJerkRanks" items="[[l.cleanJerkRanks]]" as="cjr">
                            <td class="rank">
                                <div inner-h-t-m-l="[[cjr]]"></div>
                            </td>
                        </template>
                        <td class="narrow">[[l.total]]</td>
                        <template is="dom-repeat" id="totalRanks" items="[[l.totalRanks]]" as="tr">
                            <td class="thRank">
                                <div inner-h-t-m-l="[[tr]]"></div>
                            </td>
                        </template>
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

customElements.define(ResultsFull.is, ResultsFull);