import { html } from "lit";

export function renderResultsAthleteRow(item) {
  return html`
    <tr class="${"athlete" + (item?.classname ?? "")}">
      <td class="${"start " + (item?.classname ?? "")}">
        <div class="${item?.classname}"> ${item?.startNumber}</div>
      </td>
      <td class="${"name " + (item?.classname ?? "")}">
        <div class="${"name ellipsis " + (item?.classname ?? "")}">${item?.fullName}</div>
      </td>
      <td class="category">
        <div>${item?.category}</div>
      </td>
      <td class="yob">
        <div>${item?.yearOfBirth}</div>
      </td>
      <td class="custom1">
        <div>${item?.custom1}</div>
      </td>
      <td class="custom2">
        <div>${item?.custom2}</div>
      </td>
      <td class="${"club " + (item?.flagClass ?? "")}">
        <div class="${item?.flagClass}" .innerHTML="${item?.flagURL} "></div>
        <div class="clubName">
          <div class="ellipsis" style="${"width: " + (item?.teamLength ?? "")}">${item?.teamName}</div>
        </div>
      </td>
      <td class="vspacer"></td>
      ${(item?.sattempts ?? []).map(
        (attempt) => html`
          <td class="${(attempt?.liftStatus ?? "") + " " + (attempt?.className ?? "")}">
            <div class="${(attempt?.liftStatus ?? "") + " " + (attempt?.className ?? "")}">${attempt?.stringValue}</div>
          </td>
        `
      )}
      <td class="best">
        <div .innerHTML="${item?.bestSnatch} "></div>
      </td>
      <td class="${"rank " + (item?.snatchMedal ?? "")}">
        <div .innerHTML="${item?.snatchRank} "></div>
      </td>
      <td class="vspacer"></td>
      ${(item?.cattempts ?? []).map(
        (attempt) => html`
          <td class="${(attempt?.liftStatus ?? "") + " " + (attempt?.className ?? "")}">
            <div class="${(attempt?.liftStatus ?? "") + " " + (attempt?.className ?? "")}">${attempt?.stringValue}</div>
          </td>
        `
      )}
      <td class="best">
        <div .innerHTML="${item?.bestCleanJerk}"></div>
      </td>
      <td class="${"rank " + (item?.cleanJerkMedal ?? "")}">
        <div .innerHTML="${item?.cleanJerkRank}"></div>
      </td>
      <td class="vspacer"></td>
      <td class="total">
        <div>${item?.total}</div>
      </td>
      <td class="${"totalRank " + (item?.totalMedal ?? "")}">
        <div .innerHTML="${item?.totalRank}"></div>
      </td>
      <td class="sinclair">
        <div>${item?.sinclair}</div>
      </td>
      <td class="${"sinclairRank " + (item?.sinclairMedal ?? "")}">
        <div>${item?.sinclairRank}</div>
      </td>
    </tr>
  `;
}