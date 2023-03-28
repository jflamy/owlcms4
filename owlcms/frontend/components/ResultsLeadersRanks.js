/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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
<link rel="stylesheet" type="text/css" href="local/styles/[[video]]results_[[autoversion]].css">
<div class$="wrapper [[teamWidthClass]] [[inactiveClass]]" style$="[[sizeOverride]];">
    <div style$="[[inactiveBlockStyle]]">
        <div class="competitionName">[[competitionName]]</div><br>
        <div class="nextGroup">[[t.WaitingNextGroup]]</div>
    </div>
    <div class="attemptBar" style$="[[normalHeaderDisplay]];">
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
            <div class="weight" id="weightDiv">[[weight]]<span style="font-size: 75%">&hairsp;[[t.KgSymbol]]</span>
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
    <div class="group" style$="[[normalHeaderDisplay]];">
        <div id="groupDiv">
            <span class="groupName">[[displayType]][[groupName]] &ndash; </span>[[liftsDone]]
        </div>
    </div>
    <div class="video" style$="[[videoHeaderDisplay]]">
        <div class="eventlogo"></div>
        <div class="videoheader"><span class="groupName">[[competitionName]] &ndash; [[liftsDone]]</span></div>
        <div class="federationlogo"></div>
    </div>

    <table class="results"
        style$="[[hiddenGridStyle]]; --top: calc([[resultLines]] + 1); --bottom: [[leaderLines]]; --nbRanks: [[nbRanks]]; [[leadersLineHeight]];">
        <template is="dom-if" if="[[athletes]]">
            <tr class="head">
                <!-- first row is all the row spans and the top cells -->
                <th class="groupCol" style="grid-row: span 2;">
                    <div style="display: grid; align-self: center" inner-h-t-m-l="[[t.Start]]"></div>
                </th>
                <th class="name" style="grid-row: span 2">
                    <div style="display: grid; align-self: center" inner-h-t-m-l="[[t.Name]]"></div>
                </th>
                <th class="category" style="grid-row: span 2">
                    <div style="display: grid; align-self: center" inner-h-t-m-l="[[t.Category]]"></div>
                </th>
                <th class="yob" style="grid-row: span 2">
                    <div style="display: grid; align-self: center" inner-h-t-m-l="[[t.Birth]]"></div>
                </th>
                <th class="custom1" style="grid-row: span 2">
                    <div style="display: grid; align-self: center" inner-h-t-m-l="[[t.Custom1]]"></div>
                </th>
                <th class="custom2" style="grid-row: span 2">
                    <div style="display: grid; align-self: center" inner-h-t-m-l="[[t.Custom2]]"></div>
                </th>
                <th class="club" style="grid-row: span 2">
                    <div style="display: grid; align-self: center" inner-h-t-m-l="[[t.Team]]"></div>
                </th>
                <th class="vspacer"></th>
                <th style$="grid-column: span calc(3 + [[nbRanks]] + 1);" inner-h-t-m-l="[[t.Snatch]]"></th>

                <th class="vspacer"></th>
                <th style$="grid-column: span calc(3 + [[nbRanks]] + 1);" inner-h-t-m-l="[[t.Clean_and_Jerk]]"></th>

                <th class="vspacer"></th>
                <th style$="grid-column: span calc(1 + [[nbRanks]]);" inner-h-t-m-l="[[t.Total]]"></th>

                <th class="sinclair" style="grid-row: span 2">
                    <div style="display: grid; align-self: center" inner-h-t-m-l="[[t.Sinclair]]"></div>
                </th>
                <th class="sinclairRank" style="grid-row: span 2" inner-h-t-m-l="[[t.Rank]]"></th>
            </tr>
            <tr class="head">
                <!-- second row is already partially filled from the row spans, only provide the empty cells
                    on the bottom row -->
                <th class="vspacer"></th>
                <th class="narrow">1</th>
                <th class="narrow">2</th>
                <th class="narrow">3</th>
                <th class="best" inner-h-t-m-l="[[t.Best]]"></th>
                <template is="dom-repeat" id="snatchAgeGroups" items="[[ageGroups]]" as="sag">
                    <th>[[sag]]</th>
                </template>

                <th class="vspacer"></th>
                <th class="narrow">1</th>
                <th class="narrow">2</th>
                <th class="narrow">3</th>
                <th class="best" inner-h-t-m-l="[[t.Best]]"></th>
                <template is="dom-repeat" id="cjAgeGroups" items="[[ageGroups]]" as="cjag">
                    <th>[[cjag]]</th>
                </template>
                <th class="vspacer"></th>
                <th class="narrow" inner-h-t-m-l="[[t.Total]]"></th>
                <template is="dom-repeat" id="totalAgeGroups" items="[[ageGroups]]" as="tag">
                    <th>[[tag]]</th>
                </template>

            </tr>
            <template is="dom-repeat" id="result-table" items="[[athletes]]" as="l">
                <template is="dom-if" if="[[l.isSpacer]]">
                    <tr>
                        <td class="spacer" style="grid-column: 1 / -1; justify-content: left;" inner-h-t-m-l="&nbsp;">
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
                                <div class$="[[attempt.goodBadClassName]] [[attempt.className]]">[[attempt.stringValue]]
                                </div>
                            </td>
                        </template>
                        <td class="best">
                            <div inner-h-t-m-l="[[l.bestSnatch]]"></div>
                        </td>
                        <template is="dom-repeat" id="snatchRanks" items="[[l.snatchRanks]]" as="sr">
                            <td class="rank">
                                <div inner-h-t-m-l="[[sr]]"></div>
                            </td>
                        </template>
                        <td class="vspacer"></td>
                        <template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
                            <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                <div class$="[[attempt.goodBadClassName]] [[attempt.className]]">[[attempt.stringValue]]
                                </div>
                            </td>
                        </template>
                        <td class="best">
                            <div inner-h-t-m-l="[[l.bestCleanJerk]]"></div>
                        </td>
                        <template is="dom-repeat" id="cleanJerkRanks" items="[[l.cleanJerkRanks]]" as="cjr">
                            <td class="rank">
                                <div inner-h-t-m-l="[[cjr]]"></div>
                            </td>
                        </template>
                        <td class="vspacer"></td>
                        <td class="total">[[l.total]]</td>
                        <template is="dom-repeat" id="totalRanks" items="[[l.totalRanks]]" as="tr">
                            <td class="totalRank">
                                <div inner-h-t-m-l="[[tr]]"></div>
                            </td>
                        </template>
                        <td class="sinclair">
                            <div>[[l.sinclair]]</div>
                        </td>
                        <td class="sinclairRank">
                            <div>[[l.sinclairRank]]</div>
                        </td>
                    </tr>
                </template>
            </template>
        </template>
        <tr>
            <td class="filler" style="grid-column: 1 / -1">&nbsp;</td>
        </tr>
        <template is="dom-if" if="[[leaders]]">
            <tbody class="leaders" style$="[[leadersVisibility]]">
                <tr class="head">
                    <td style="grid-column: 1 / -1; justify-content: left;"
                        inner-h-t-m-l="[[t.Leaders]] [[categoryName]]">
                    </td>
                </tr>
                <tr>
                    <td class="spacer" style$="grid-column: 1 / -1; justify-content: left; [[leadersVisibility]];"
                        inner-h-t-m-l="&nbsp;">
                    </td>
                </tr>
                <template is="dom-repeat" id="result-table" items="[[leaders]]" as="l">
                    <template is="dom-if" if="[[!l.isSpacer]]">
                        <tr class="athlete">
                            <td class="groupCol">
                                <div>[[l.group]]</div>
                            </td>
                            <td class$="name [[l.classname]]">
                                <div class="ellipsis">[[l.fullName]]</div>
                            </td>
                            <td class="category">
                                <div>[[l.category]]</div>
                            </td>
                            <td class="yob" style$="[[leadersVisibility]]">
                                <div>[[l.yearOfBirth]]</div>
                            </td>
                            <td class="custom1" style$="[[leadersVisibility]]">
                                <div>[[l.custom1]]</div>
                            </td>
                            <td class="custom2" style$="[[leadersVisibility]]">
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
                            <td class="best" style$="[[leadersVisibility]]">
                                <div inner-h-t-m-l="[[l.bestSnatch]]"></div>
                            </td>
                            <template is="dom-repeat" id="snatchRanks" items="[[l.snatchRanks]]" as="sr">
                                <td class="rank" style$="[[leadersVisibility]]">
                                    <div inner-h-t-m-l="[[sr]]"></div>
                                </td>
                            </template>
                            <td class="vspacer" style$="[[leadersVisibility]]"></td>
                            <template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
                                <td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
                                    <div>[[attempt.stringValue]]</div>
                                </td>
                            </template>
                            <td class="best" style$="[[leadersVisibility]]">
                                <div inner-h-t-m-l="[[l.bestCleanJerk]]"></div>
                            </td>
                            <template is="dom-repeat" id="cleanJerkRanks" items="[[l.cleanJerkRanks]]" as="cjr">
                                <td class="rank" style$="[[leadersVisibility]]">
                                    <div inner-h-t-m-l="[[cjr]]"></div>
                                </td>
                            </template>
                            <td class="vspacer"></td>
                            <td class="total" style$="[[leadersVisibility]]">[[l.total]]</td>
                            <template is="dom-repeat" id="totalRanks" items="[[l.totalRanks]]" as="tr">
                                <td class="totalRank" style$="[[leadersVisibility]]">
                                    <div inner-h-t-m-l="[[tr]]"></div>
                                </td>
                            </template>
                            <td class="sinclair" style$="[[leadersVisibility]]">
                                <div>[[l.sinclair]]</div>
                            </td>
                            <td class="sinclairRank" style$="[[leadersVisibility]]">
                                <div>[[l.sinclairRank]]</div>
                            </td>
                        </tr>
                    </template>
                </template>
            </tbody>
        </template>
    </table>
    <template is="dom-if" if="[[records]]">
        <div
            style$="font-size: calc(var(--tableFontSize) * var(--recordsFontRatio)); [[hiddenBlockStyle]]; [[recordsDisplay]]; height: 100%;">
            <div class="recordsFiller">&nbsp;</div>

            <div class="recordRow" style$="--nbRecords: [[records.nbRecords]];">
                <div>
                    <div class="recordName recordTitle">[[t.records]]</div>
                    <div class="recordLiftTypeSpacer">&nbsp;</div>
                    <template is="dom-repeat" id="result-table" items="[[records.recordNames]]" as="n">
                        <div class="recordName">[[n]]</div>
                    </template>
                </div>

                <template is="dom-repeat" id="result-table" items="[[records.recordTable]]" as="c">
                    <div class="recordBox">
                        <div class="recordCat" inner-h-t-m-l="[[c.cat]]"></div>
                        <div>
                            <div class="recordLiftType">[[t.recordS]]</div>
                            <div class="recordLiftType">[[t.recordCJ]]</div>
                            <div class="recordLiftType">[[t.recordT]]</div>
                        </div>
                        <template is="dom-repeat" id="result-table" items="[[c.records]]" as="r">
                            <div>
                                <div class$="recordCell [[r.snatchHighlight]]">[[r.SNATCH]]</div>
                                <div class$="recordCell [[r.cjHighlight]]">[[r.CLEANJERK]]</div>
                                <div class$="recordCell [[r.totalHighlight]]">[[r.TOTAL]]</div>
                            </div>
                        </template>
                    </div>
                </template>

                <div class$="recordNotification [[recordKind]]">[[recordMessage]]</div>
            </div>
        </div>
    </template>
</div>`;
    }

    ready() {
        console.debug("ready");
        super.ready();
        document.body.setAttribute("theme", "dark");
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

    doBreak(showWeights) {
        console.debug("break");
        this.$.groupDiv.style.visibility = "hidden";
        this.$.fullNameDiv.style.visibility = "visible";
        this.$.fullNameDiv.style.display = "flex";
        this.$.startNumberDiv.style.display = "none";
        this.$.teamNameDiv.style.display = "none";
        this.$.attemptDiv.style.display = "none";
        if (showWeights) {
            this.$.weightDiv.style.display = "block";
            this.$.breakTimerDiv.style.display = "flex";
        } else {
            this.$.weightDiv.style.display = "none";
            this.$.breakTimerDiv.style.display = "none";
        }

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