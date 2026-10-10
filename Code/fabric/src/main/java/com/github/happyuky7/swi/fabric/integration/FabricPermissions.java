package com.github.happyuky7.swi.fabric.integration;

import com.github.happyuky7.swi.mc.SwiCore;
//#if MC >= 1.18
import me.lucko.fabric.api.permissions.v0.Permissions;
//#endif

/**
 * Admin commands honour {@code separeworlditems.admin} through fabric-permissions-api (LuckPerms, etc.),
 * falling back to operator level 2. Not available on 1.16.5.
 */
public final class FabricPermissions {

    public static final String ADMIN = "separeworlditems.admin";

    private FabricPermissions() {
    }

    public static void install(SwiCore core) {
        //#if MC >= 1.18
        core.setAdminCheck(source -> Permissions.check(source, ADMIN, 2));
        //#endif
    }
}
