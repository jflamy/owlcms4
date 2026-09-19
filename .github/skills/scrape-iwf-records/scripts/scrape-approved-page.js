const result = await page.evaluate(async () => {
  const ageGroupMap = {
    Senior: ['SR', 15, 999],
    Junior: ['JR', 15, 20],
    Youth: ['Youth', 13, 17],
    U15: ['U15', 0, 15],
  };
  const text = (element) => element?.textContent?.trim() || '';
  const options = (selector) => [...document.querySelectorAll(`${selector} option`)]
    .filter((option) => text(option))
    .map((option) => [text(option), option.value]);
  const ageGroups = options('[name="ranking_agegroup"]');
  const genders = options('#ranking_gender');

  const pages = await Promise.all(ageGroups.flatMap(([ageName, ageValue]) =>
    genders.map(async ([genderName, genderValue]) => {
      const [ageCode, ageLow, ageUpper] = ageGroupMap[ageName] || [ageName, 0, 0];
      const genderCode = /Men|Male/.test(genderName) ? 'M' : 'F';
      const params = new URLSearchParams({
        ranking_curprog: 'current',
        ranking_agegroup: ageValue,
        ranking_gender: genderValue,
      });
      const response = await fetch(`/results/world-records/?${params}`, {
        credentials: 'include',
      });
      if (!response.ok) {
        throw new Error(`${ageName} ${genderName}: HTTP ${response.status}`);
      }
      return {
        document: new DOMParser().parseFromString(await response.text(), 'text/html'),
        ageCode,
        ageLow,
        ageUpper,
        genderCode,
      };
    })
  ));

  const records = pages.flatMap(({ document: resultPage, ageCode, ageLow, ageUpper, genderCode }) => {
    const headers = [...resultPage.querySelectorAll('div.results__title h2')]
      .filter((header) => /kg/i.test(text(header)) && !/World Records/i.test(text(header)));
    const cards = [...resultPage.querySelectorAll('div.card')]
      .filter((card) => card.querySelector('div.col-md-2.print__2.title'));
    let previousWeight = 0;

    return headers.flatMap((header, categoryIndex) => {
      const match = text(header).match(/(\+?)(\d+)\s*kg/i);
      if (!match) return [];

      const weight = Number(match[2]);
      const bwLow = previousWeight;
      const bwUpper = match[1] ? `>${weight}` : String(weight);
      previousWeight = weight;

      return cards.slice(categoryIndex * 3, categoryIndex * 3 + 3).flatMap((card) => {
        const title = card.querySelectorAll('div.col-md-2.print__2.title p');
        if (title.length < 2) return [];

        const liftText = text(title[0]);
        const recordValue = Number.parseInt(
          text(title[1]).replace('Record:', '').replace('kg', '').trim(),
          10
        );
        const lift = /Snatch/i.test(liftText)
          ? 'Snatch'
          : /C&J|Clean|Jerk/i.test(liftText)
            ? 'Clean & Jerk'
            : /Total/i.test(liftText)
              ? 'Total'
              : '';
        if (!lift || !Number.isFinite(recordValue)) return [];

        const detail = card.querySelector('div.col-md-7.print__7');
        const detailLines = [...(detail?.querySelectorAll('p') || [])].map(text);
        const eventValue = (detailLines.find((line) => line.startsWith('Event Place & Date:')) || '')
          .replace('Event Place & Date:', '')
          .trim();
        const [dateValue = '', placeValue = ''] = eventValue.split(' - ', 2);
        const flagNation = detail?.querySelector('span.flag img')?.getAttribute('alt')?.trim() || '';

        return [{
          Federation: 'IWF',
          RecordName: 'World',
          AgeGroup: ageCode,
          'M/F': genderCode,
          ageLow,
          ageUpper,
          bwLow,
          bwUpper,
          Lift: lift,
          Record: recordValue,
          Name: text(card.querySelector('div.col-md-3.print__3.not__cell__767')),
          Born: (detailLines.find((line) => line.startsWith('Born:')) || '')
            .replace('Born:', '')
            .trim(),
          Nation: flagNation || (detailLines.find((line) => line.startsWith('Nation:')) || '')
            .replace('Nation:', '')
            .trim(),
          Date: dateValue.trim(),
          Place: placeValue.trim() === 'World Standard' ? '' : placeValue.trim(),
          Event: '',
          Group: '',
        }];
      });
    });
  });

  await fetch('http://127.0.0.1:8765', {
    method: 'POST',
    mode: 'no-cors',
    headers: { 'Content-Type': 'text/plain' },
    body: JSON.stringify(records),
  });

  const counts = Object.fromEntries(
    [...new Set(records.map((record) => `${record.AgeGroup}-${record['M/F']}`))]
      .map((key) => [key, records.filter((record) => `${record.AgeGroup}-${record['M/F']}` === key).length])
  );
  return { records: records.length, counts };
});
return result;
