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
    private MySubsystem()
    {
        boolean success = true;

        try {
            // I would initialize motors / sensors here
        }
        catch (Exception e)
        {
            success = false;
            logException("Something went wrong: ", e); // Not actually doing anything yet
        }

        logInitialized(success);
    }

    // private mLoop - put code that does something here
    private ILoop mLoop = new ILoop() // why is this ILoop - is there a new prefix, like k and m, that I don't know about?
    {
        @Override
        public void onStart(double timestamp)
        {

        }

        @Override
        public void onLoop(double timestamp)
        {

        }

        @Override
        public void onStop(double timestamp)
        {

        }

    }; // I don't get why there's a semicolon, but my theme seems to say it's needed
    // which is weird i don't get why my theme is working for syntax errors but not my actual syntax error extension (language support)

    /* I don't fully understand what Override does - is it
     * overriding Subsystem.registerEnabledLoops? and does
     * that mean that something like MySubsystem.getName()
     * is the same as Subsystem.getName()?
     */
    @Override
    public void registerEnabledLoops(ILooper enabledLooper)
    {
        enabledLooper.register(mLoop);
    }

    // other methods

    // State transfer methods

}