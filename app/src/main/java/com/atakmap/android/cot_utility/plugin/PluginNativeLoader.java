
package com.atakmap.android.cot_utility.plugin;

import java.io.File;

import android.content.Context;

/*
    Copyright 2020 Raytheon BBN Technologies

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
public class PluginNativeLoader {

    private static final String TAG = "NativeLoader";
    private static String ndl = null;

    /**
    * If a plugin wishes to make use of this class, they will need to copy it into their plugin.
    * The classloader that loads this class is a key component of getting System.load to work 
    * properly.   If it is desirable to use this in a plugin, it will need to be a direct copy in a
    * non-conflicting package name.
    */
    synchronized static public void init(final Context context) {
        if (ndl == null) {
            try {
                final String nativeDir = context.getPackageManager()
                        .getApplicationInfo(context.getPackageName(),
                                0).nativeLibraryDir;

                ndl = nativeDir;
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "native library loading will fail, unable to grab the nativeLibraryDir from the package name");
            }

        }
    }

    /**
    * Security guidance from our recent audit:
    * Pass an absolute path to System.load(). Avoid System.loadLibrary() because its behavior 
    * depends upon its implementation which often relies on environmental features that can be 
    * manipulated. Use only validated, sanitized absolute paths.
    */

    public static void loadLibrary(final String name) {
        if (ndl != null) {
            final String lib = ndl + File.separator
                    + System.mapLibraryName(name);
            if (new File(lib).exists()) {
                System.load(lib);
                return;
            }
        } else {
            throw new IllegalArgumentException("NativeLoader not initialized");
        }

    }

}
