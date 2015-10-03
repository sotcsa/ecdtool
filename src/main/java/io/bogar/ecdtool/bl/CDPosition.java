package io.bogar.ecdtool.bl;

public class CDPosition {
	
	private int position = 0;

	public CDPosition() {
	}
	
	public CDPosition(int position) {
		this.position = position;
	}
	
	public CDPosition(int min, int sec, int tick) {
		position = tick + 75 * (sec + 60 * min);
	}

	public CDPosition(String minutes, String seconds, String ticks) {
		position = parse(minutes, seconds, ticks);
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getMinutes() {
		int minutes = (position/(60*75));
		return minutes;
	}
	
	public int getSeconds() {
		int seconds = ((position/75) % 60);
		return seconds;
	}
	
	public int getTicks() {
		int ticks = (position % 75);
		return ticks;
	}
	
	public String toIndexString() {
		return String.format("%02d:%02d:%02d", getMinutes(), getSeconds(), getTicks());
	}

	@Override
	public String toString() {
		return String.format("%02d:%02d.%02d", getMinutes(), getSeconds(), getTicks());
	}
	
	public static int parse(String minutes, String seconds, String ticks) {
		int value = Integer.parseInt(ticks, 10);
		value += 75*Integer.parseInt(seconds, 10);
		value += 75*60*Integer.parseInt(minutes, 10);
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + position;
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
		final CDPosition other = (CDPosition) obj;
		if (position != other.position)
			return false;
		return true;
	}
	
}
