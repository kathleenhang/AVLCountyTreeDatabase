
public class County
{
	int countyCode;
	int population;
	String countyName;
	
	County()
	{
		
	}
	
	County(int countyCode, int population, String countyName)
	{
		setCountyCode(countyCode);
		setPopulation(population);
		setCountyName(countyName);
	}

	public int getCountyCode() {
		return countyCode;
	}

	public void setCountyCode(int countyCode) {
		this.countyCode = countyCode;
	}

	public int getPopulation() {
		return population;
	}

	public void setPopulation(int population) {
		this.population = population;
	}

	public String getCountyName() {
		return countyName;
	}

	public void setCountyName(String countyName) {
		this.countyName = countyName;
	}
	
	public String toString()
	{
		return "Code: " + getCountyCode() 
			+ " - Population: " + getPopulation()
			+ " - Name: " + getCountyName();
	}
}
