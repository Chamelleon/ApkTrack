/*
 * Copyright (c) 2015
 *
 * ApkTrack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ApkTrack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ApkTrack.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.kwiatkowski.ApkTrack;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Comparator;

public class InstalledApp implements Comparable<InstalledApp>, Parcelable
{
    private String package_name;
    private String display_name;
    private String version;
    private String latest_version = null;
    private Drawable icon;
    private boolean last_ckeck_error = false;
    private boolean system_app;
    private String last_check_date = null;

    // Volatile fields (won't be persisted)
    private boolean currently_checking = false;

    public InstalledApp(String package_name, String version, String display_name, boolean system_app, Drawable icon)
    {
        this.package_name = package_name;
        this.version = version;
        this.display_name = display_name;
        this.icon = icon;
        this.system_app = system_app;
    }

    public InstalledApp(Parcel in) {
        readFromParcel(in);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        public InstalledApp createFromParcel(Parcel in) {
            return new InstalledApp(in);
        }

        public InstalledApp[] newArray(int size) {
            return new InstalledApp[size];
        }

    };

    public String getPackageName() {
        return package_name;
    }

    public String getDisplayName() {
        return display_name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(BitmapDrawable icon) {
        this.icon = icon;
    }

    public void setLatestVersion(String latest_version) {
        this.latest_version = latest_version;
    }

    public String getLatestVersion() {
        return latest_version;
    }

    public String getLastCheckDate() {
        return last_check_date;
    }

    public void setLastCheckDate(String last_check_date) {
        this.last_check_date = last_check_date;
    }

    public boolean isLastCheckFatalError() {
        return last_ckeck_error;
    }

    public void setLastCheckFatalError(boolean last_ckeck_error) {
        this.last_ckeck_error = last_ckeck_error;
    }

    public boolean isCurrentlyChecking() {
        return currently_checking;
    }

    public void setCurrentlyChecking(boolean currently_checking) {
        this.currently_checking = currently_checking;
    }

    public boolean isSystemApp() {
        return system_app;
    }

    public int updateCompareTo(InstalledApp a)
    {
        // One of the applications was never checked:
        if (getLatestVersion() == null && a.getLatestVersion() != null) {
            return -1;
        }
        else if (getLatestVersion() != null && a.getLatestVersion() == null) {
            return 1;
        }
        else if (getLatestVersion() == null) { // Both are unchecked
            return compareTo(a);
        }

        // One of the applications has an error
        if (isLastCheckFatalError() && !a.isLastCheckFatalError()) {
            return 1;
        }
        else if (!isLastCheckFatalError() && a.isLastCheckFatalError()) {
            return -1;
        }
        else if (isLastCheckFatalError()) {
            return compareTo(a);
        }

        // Both applications have been checked and no errors
        if (getVersion().equals(getLatestVersion()) && !a.getVersion().equals(a.getLatestVersion())) {
            return 1;
        }
        else if (!getVersion().equals(getLatestVersion()) && a.getVersion().equals(a.getLatestVersion())) {
            return -1;
        }
        else return compareTo(a);
    }

    boolean isUpdateAvailable() {
        if (version == null || latest_version == null) {
            return false;
        }
        if (last_ckeck_error) {
            return false;
        }
        return !version.equals(getLatestVersion());
    }

    @Override
    public int compareTo(InstalledApp installedApp) {
        return display_name.compareTo(installedApp.display_name);
    }

    public int systemUpdateCompareTo(InstalledApp a)
    {
        if (!isSystemApp() && a.isSystemApp()) {
            return -1;
        }
        else if (isSystemApp() && !a.isSystemApp()) {
            return 1;
        }
        else return updateCompareTo(a);
    }

    /**
     * Define equality between two InstalledApp objects as identical package names.
     * This is not true from a language standpoint, but it makes sense in the context
     * of this application: we assume that two applications with the same package name
     * cannot coexist on the device.
     * @param o The object to compare this instance with.
     * @return <code>true</code> if the specified object is equal to this <code>Object</code>; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof InstalledApp) {
            return this.package_name.equals(((InstalledApp) o).getPackageName());
        }
        else {
            return super.equals(o);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeString(package_name);
        out.writeString(display_name);
        out.writeString(version);
        out.writeString(latest_version);
        out.writeInt(last_ckeck_error ? 1 : 0); // No API for booleans?
        out.writeInt(system_app ? 1 : 0);
        out.writeString(last_check_date);
    }

    private void readFromParcel(Parcel in)
    {
        package_name = in.readString();
        display_name = in.readString();
        version = in.readString();
        latest_version = in.readString();
        last_ckeck_error = in.readInt() == 1;
        system_app = in.readInt() == 1;
        last_check_date = in.readString();
    }
}

/**
 * A comparator used to sort applications based on whether they are system applications or not.
 */
class SystemComparator implements Comparator<InstalledApp>
{
    /**
     * This comparator sorts InstalledApps in the following way:
     * - System applications are put at the end of the list and user applications are put at the beginning.
     * - Between them, system and user apps are sorted alphabetically.
     * @param a1 The first app to compare
     * @param a2 The second app to compare
     * @return A negative number if a1 < a2, a positive number if a1 > a2, 0 if they are deemed equal.
     */
    public int compare(InstalledApp a1, InstalledApp a2)
    {
        if (!a1.isSystemApp() && a2.isSystemApp()) {
            return -1;
        }
        else if (a1.isSystemApp() && !a2.isSystemApp()) {
            return 1;
        }
        else {
            return a1.compareTo(a2);
        }
    }
}

/**
 * A comparator used to sort applications based on whether they are updated or not.
 */
class UpdatedComparator implements Comparator<InstalledApp>
{
    /**
     * This comparator sorts InstalledApps in the following way:
     * - Never checked applications or checks with fatal errors are put at the bottom.
     * - Updated applications are put at the top.
     * - Applications in the same group are sorted alphabetically.
     * @param a1 The first app to compare
     * @param a2 The second app to compare
     * @return A negative number if a1 < a2, a positive number if a1 > a2, 0 if they are deemed equal.
     */
    public int compare(InstalledApp a1, InstalledApp a2)
    {
        return a1.updateCompareTo(a2);
    }
}

/**
 * A comparator used to sort applications based on whether they are system applications and updated or not.
 */
class UpdatedSystemComparator implements Comparator<InstalledApp>
{
    /**
     * This comparator sorts InstalledApps in the following way:
     * - System applications are put at the end of the list and user applications are put at the beginning.
     * - Non-system updated applications are put at the top and applications with update errors are put at the bottom.
     * - Between them, system, user, and user updated apps are sorted alphabetically.
     * @param a1 The first app to compare
     * @param a2 The second app to compare
     * @return A negative number if a1 < a2, a positive number if a1 > a2, 0 if they are deemed equal.
     */
    public int compare(InstalledApp a1, InstalledApp a2)
    {
        return a1.systemUpdateCompareTo(a2);
    }
}

/**
 * Sorts application in alphabetical order
 */
class AlphabeticalComparator implements Comparator<InstalledApp>
{
    public int compare(InstalledApp a1, InstalledApp a2) {
        return a1.compareTo(a2);
    }
}