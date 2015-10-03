package io.bogar.ecdtool.bl;


public class CDIndex {

	protected int track;

	protected int no;

	protected CDPosition position;

	public CDIndex(int track, int index, int min, int sec, int tick) {
		position = new CDPosition(min, sec, tick);
		setTrack(track);
		setNo(index);
	}

	public CDIndex(int track, int index, String smin, String ssec, String stick) {
		this(track, index, Integer.parseInt(smin, 10), Integer.parseInt(ssec,
				10), Integer.parseInt(stick, 10));
	}

	public int getNo() {
		return no;
	}

	public void setNo(int index) {
		this.no = index;
	}

	public int getTrack() {
		return track;
	}

	public void setTrack(int track) {
		this.track = track;
	}

	public CDPosition getPosition() {
		return position;
	}

	public void setPosition(CDPosition where) {
		this.position = where;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + no;
		result = prime * result
				+ ((position == null) ? 0 : position.hashCode());
		result = prime * result + track;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final CDIndex other = (CDIndex) obj;
		if (no != other.no)
			return false;
		if (position == null) {
			if (other.position != null)
				return false;
		} else if (!position.equals(other.position))
			return false;
		if (track != other.track)
			return false;
		return true;
	}

}
