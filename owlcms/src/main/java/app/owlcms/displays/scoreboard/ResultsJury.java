package app.owlcms.displays.scoreboard;

import app.owlcms.nui.displays.scoreboards.WarmupNoLeadersPage;

@SuppressWarnings("serial")
public class ResultsJury extends ResultsNoLeaders {
	
	public ResultsJury(WarmupNoLeadersPage page) {
		super(page);
	}

	@Override
	public boolean isJury() {
		return true;
	}

}
