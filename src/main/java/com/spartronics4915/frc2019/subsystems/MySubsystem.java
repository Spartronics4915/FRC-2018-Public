public class MySubsystem extends Subsystem
{
    private static MySubsystem mInstance = null;

    public static MySubsystem getInstance();
    {
        if (mInstance == null)
        {
            mInstance = new MySubsystem();
        }
        return mInstance;
    }

    // WantedStates

    // SystemStates

    // Set variables

    // private MySubsystem() - just initiates it

    // private mLoop - put code that does something here

    // other methods

    // State transfer methods

}