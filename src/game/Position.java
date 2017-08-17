package game;

public class Position {

	private int y;
	private int x;
	
	public Position (int y, int x){
		this.y = y;
		this.x = x;
	}

	public Position(Position other) {
		y = other.getY();
		x = other.getX();
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}
	
	public void set(int y, int x) {
		this.y = y;
		this.x = x;
	}
	
	public void set(Position c) {
		y = c.y;
		x = c.x;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
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
		Position other = (Position) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}
}
