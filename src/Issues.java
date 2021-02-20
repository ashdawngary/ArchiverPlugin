public class Issues {

  private final String message;

  public Issues(String str) {
    this.message = str;
  }

  @Override
  public String toString(){
   return this.message;
  }
}
