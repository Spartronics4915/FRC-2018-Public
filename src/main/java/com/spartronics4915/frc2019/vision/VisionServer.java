
package com.spartronics4915.frc2019.vision;

import com.spartronics4915.lib.util.ILooper;
import com.spartronics4915.lib.util.ILoop;
import com.spartronics4915.lib.util.Logger;

import com.spartronics4915.frc2019.Constants;

import edu.wpi.first.wpilibj.Notifier;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTable;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import com.spartronics4915.lib.geometry.Translation2d;

import com.spartronics4915.lib.geometry.Pose2d;
// Does not have ridgidTransform 2d

/*
 * This code runs all of the robot's loops. Loop objects are stored in a List
 * object. They are started when the robot
 * powers up and stopped after the match.
 * 
 * NOTE: Removed all @override from file, nessissary?
 * 
 */
public class VisionServer implements ILoop {

    public final double kPeriod = Constants.kLooperDt;

    private boolean running_;

    static VisionServer instance_ = null;
    // RobotState robot_state_ = RobotState.getInstance();
    // Goaltracker

    String defaultValue = "null";

    TargetInfo update_;
    TargetInfo update;

    final NetworkTable table = NetworkTableInstance.getDefault().getTable(Constants.kVisionTableName);

    private NetworkTableEntry mVisionTargetAngleEntry = null;
    private NetworkTableEntry mVisionTargetElevationEntry = null;
    private NetworkTableEntry mVisionStatusEntry = null;
    private NetworkTableEntry mVisionClockEntry = null;

    // TODO: Double-check with conventions...are underscores in proper place?
    private GoalTracker goal_tracker_ = new GoalTracker();

    public static VisionServer getInstance() {
        if (instance_ == null) {
            instance_ = new VisionServer();
        }
        return instance_;
    }

    /*
     * VisionServer() { }
     */

    public void onStart(double timestamp) {
    }

    public void onLoop(double timestamp) {
        update_ = generateUpdate();
        // Look at smartDashboard and create a targetinfo.

        synchronized (this) {
            if (update_ == null) {
                return;
            }
            // Re-null last update; Don't want to contine to add the same update to the
            // system
            update = update_;
            update_ = null;
        }
        goal_tracker_.addVisionUpdate(update.getTimestamp(), update);
        // goal_tracker_.addVisionUpdate(update.getTimestamp(), update.getTargets());
        // Add to goaltracker
    }

    public void onStop(double timestamp) {
        // no-op
    }

    public TargetInfo generateUpdate() {
        // Pull entrys from SmartDashboard
        mVisionTargetAngleEntry = table.getEntry(Constants.kVisionTargetAngleName);
        mVisionTargetElevationEntry = table.getEntry(Constants.kVisionTargetElevationName);
        mVisionStatusEntry = table.getEntry(Constants.kVisionStatusName);
        mVisionClockEntry = table.getEntry(Constants.kVisionClockName);

        // Pull data from SmartDashboard

        double dx = mVisionTargetAngleEntry.getNumber(0).doubleValue();
        // TODO: Change to reflect targetInfo
        double dy = mVisionTargetElevationEntry.getNumber(0).doubleValue();
        String status = mVisionStatusEntry.getString(defaultValue);
        double timestamp = mVisionClockEntry.getNumber(0).doubleValue();

        if (status != "null") {

            TargetInfo target = new TargetInfo(dx, dy, timestamp);

            return target;

        } else {

            Logger.warning("Error when generating a vision update");
            return null;
        }

    }

}
