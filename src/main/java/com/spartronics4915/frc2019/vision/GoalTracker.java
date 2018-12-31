package com.spartronics4915.frc2019.vision;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.spartronics4915.frc2019.Constants;

import com.spartronics4915.lib.geometry.Pose2d;

import com.spartronics4915.lib.geometry.Translation2d;
import com.spartronics4915.lib.geometry.Rotation2d;
import com.spartronics4915.lib.util.InterpolatingTreeMap;
import com.spartronics4915.lib.util.InterpolatingDouble;

/**
 * This is used in the event that multiple goals are detected to judge all goals
 * based on timestamp, stability, and continuation of previous goals (i.e. if a
 * goal was detected earlier and has changed locations). This allows the robot
 * to make consistent decisions about which goal to aim at and to smooth out
 * jitter from vibration of the camera.
 * 
 * @see GoalTrack.java
 */
public class GoalTracker
{

    /**
     * Track reports contain all of the relevant information about a given goal
     * track.
     */

    // TODO: Find a better home for these varibles

    private Rotation2d camera_pitch_correction_;
    private Rotation2d camera_yaw_correction_;

    double differential_height_;

    private InterpolatingTreeMap<InterpolatingDouble, Pose2d> field_to_vehicle_;

    private static final Pose2d kVehicleToCamera = new Pose2d(
            new Translation2d(Constants.kCameraXOffset, Constants.kCameraYOffset), new Rotation2d());

    private static final int kObservationBufferSize = 100;

    public static class TrackReport
    {

        // Translation from the field frame to the goal
        public Translation2d field_to_goal;

        // The timestamp of the latest time that the goal has been observed
        public double latest_timestamp;

        // The percentage of the goal tracking time during which this goal has
        // been observed (0 to 1)
        public double stability;

        // The track id
        public int id;

        public TrackReport(GoalTrack track)
        {
            this.field_to_goal = track.getSmoothedPosition();
            this.latest_timestamp = track.getLatestTimestamp();
            this.stability = track.getStability();
            this.id = track.getId();
        }
    }

    /**
     * TrackReportComparators are used in the case that multiple tracks are active
     * (e.g. we see or have recently seen multiple goals). They contain heuristics
     * used to pick which track we should aim at by calculating a score for each
     * track (highest score wins).
     */
    public static class TrackReportComparator implements Comparator<TrackReport>
    {

        // Reward tracks for being more stable (seen in more frames)
        double mStabilityWeight;
        // Reward tracks for being recently observed
        double mAgeWeight;
        double mCurrentTimestamp;
        // Reward tracks for being continuations of tracks that we are already
        // tracking
        double mSwitchingWeight;
        int mLastTrackId;

        public TrackReportComparator(double stability_weight, double age_weight, double switching_weight,
                int last_track_id, double current_timestamp)
        {
            this.mStabilityWeight = stability_weight;
            this.mAgeWeight = age_weight;
            this.mSwitchingWeight = switching_weight;
            this.mLastTrackId = last_track_id;
            this.mCurrentTimestamp = current_timestamp;
        }

        double score(TrackReport report)
        {
            double stability_score = mStabilityWeight * report.stability;
            double age_score = mAgeWeight
                    * Math.max(0, (Constants.kMaxGoalTrackAge - (mCurrentTimestamp - report.latest_timestamp))
                            / Constants.kMaxGoalTrackAge);
            double switching_score = (report.id == mLastTrackId ? mSwitchingWeight : 0);
            return stability_score + age_score + switching_score;
        }

        @Override
        public int compare(TrackReport o1, TrackReport o2)
        {
            double diff = score(o1) - score(o2);
            // Greater than 0 if o1 is better than o2
            if (diff < 0)
            {
                return 1;
            }
            else if (diff > 0)
            {
                return -1;
            }
            else
            {
                return 0;
            }
        }
    }

    List<GoalTrack> mCurrentTracks = new ArrayList<>();
    int mNextId = 0;

    public GoalTracker()
    {

    }

    public void reset()
    {
        mCurrentTracks.clear();

        camera_pitch_correction_ = Rotation2d.fromDegrees(-Constants.kCameraPitchAngleDegrees);
        camera_yaw_correction_ = Rotation2d.fromDegrees(-Constants.kCameraYawAngleDegrees);

        differential_height_ = Constants.kBoilerTargetTopHeight - Constants.kCameraZOffset;

        field_to_vehicle_ = new InterpolatingTreeMap<>(kObservationBufferSize);
        field_to_vehicle_.put(new InterpolatingDouble(0.0), new Pose2d());
    }

    // TODO: This function needs to be double-checked that I am plugging the correct
    // Translation2D
    public void update(double timestamp, Translation2d field_to_goals)
    {
        // Try to update existing tracks
        boolean hasUpdatedTrack = false;
        for (GoalTrack track : mCurrentTracks)
        {
            if (!hasUpdatedTrack)
            {
                if (track.tryUpdate(timestamp, field_to_goals))
                {
                    hasUpdatedTrack = true;
                }
            }
            else
            {
                track.emptyUpdate();
            }
        }

        // Prune any tracks that have died
        for (Iterator<GoalTrack> it = mCurrentTracks.iterator(); it.hasNext();)
        {
            GoalTrack track = it.next();
            if (!track.isAlive())
            {
                it.remove();
            }
        }
        // If all tracks are dead, start new tracks for any detections
        if (mCurrentTracks.isEmpty())
        {
            mCurrentTracks.add(GoalTrack.makeNewTrack(timestamp, field_to_goals, mNextId));
            ++mNextId;
        }
    }

    public boolean hasTracks()
    {
        return !mCurrentTracks.isEmpty();
    }

    public List<TrackReport> getTracks()
    {
        List<TrackReport> rv = new ArrayList<>();
        for (GoalTrack track : mCurrentTracks)
        {
            rv.add(new TrackReport(track));
        }
        return rv;
    }

    public void addVisionUpdate(double timestamp, TargetInfo vision_update)
    {
        Translation2d field_to_goals = new Translation2d();

        Pose2d field_to_camera = getFieldToCamera(timestamp);

        if (!(vision_update == null || vision_update.getTimestamp() != 0))
        {
            double ydeadband = (vision_update.getY() > -Constants.kCameraDeadband
                    && vision_update.getY() < Constants.kCameraDeadband) ? 0.0 : vision_update.getY();

            // Compensate for camera yaw
            double xyaw = vision_update.getX() * camera_yaw_correction_.cos()
                    + ydeadband * camera_yaw_correction_.sin();
            double yyaw = ydeadband * camera_yaw_correction_.cos()
                    - vision_update.getX() * camera_yaw_correction_.sin();
            double zyaw = vision_update.getZ();

            // Compensate for camera pitch
            double xr = zyaw * camera_pitch_correction_.sin() + xyaw * camera_pitch_correction_.cos();
            double yr = yyaw;
            double zr = zyaw * camera_pitch_correction_.cos() - xyaw * camera_pitch_correction_.sin();

            // find intersection with the goal
            if (zr > 0)
            {
                double scaling = differential_height_ / zr;
                double distance = Math.hypot(xr, yr) * scaling + Constants.kBoilerRadius;
                Rotation2d angle = new Rotation2d(xr, yr, true);
                field_to_goals = (field_to_camera
                        .transformBy(Pose2d
                                .fromTranslation(new Translation2d(distance * angle.cos(), distance * angle.sin())))
                        .getTranslation());
            }

        }
        synchronized (this)
        {
            update(timestamp, field_to_goals);
        }
    }

    public synchronized Pose2d getFieldToCamera(double timestamp)
    {
        return getFieldToVehicle(timestamp).transformBy(kVehicleToCamera);
    }

    public synchronized Pose2d getFieldToVehicle(double timestamp)
    {
        return field_to_vehicle_.getInterpolated(new InterpolatingDouble(timestamp));
    }

}
