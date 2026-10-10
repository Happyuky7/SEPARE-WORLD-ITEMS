//#if MC >= 1.21
package com.github.happyuky7.swi.mc.integration.regions;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.mc.compat.Mc;
import com.github.happyuky7.swi.mc.region.ModRegions;
import com.github.happyuky7.swi.mc.region.RegionSource;
import io.github.flemmli97.flan.api.ClaimHandler;
import io.github.flemmli97.flan.api.data.IPermissionContainer;
import io.github.flemmli97.flan.claim.Claim;
import net.minecraft.server.level.ServerPlayer;

/**
 * Flan claims. Flan stores claims with an id, an optional name, an owner and an admin flag; sub-claims
 * inherit the group of their parent unless configured themselves.
 *
 * <pre>
 * regions:
 *   flan:
 *     claims: { "&lt;claim uuid or name&gt;": vip }   # a specific claim
 *     owners: { "&lt;owner uuid&gt;": base }           # every claim of a player
 *     admin-claims: spawn                           # admin (server) claims
 *     any-claim: claims                             # any other claim
 * </pre>
 */
public final class FlanRegions implements RegionSource {

    @Override
    public String id() {
        return "flan";
    }

    @Override
    public Match match(ServerPlayer player, DataNode config) {
        IPermissionContainer container = ClaimHandler.getPermissionStorage(Mc.level(player)).getForPermissionCheck(player.blockPosition());
        if (!(container instanceof Claim)) {
            return null;
        }
        Claim claim = (Claim) container;
        String group = null;
        for (Claim c = claim; c != null && group == null; c = c.parentClaim()) {
            group = ModRegions.lookup(config.getNode("claims"), String.valueOf(c.getClaimID()), c.getClaimName());
        }
        if (group == null && claim.getOwner() != null) {
            group = ModRegions.lookup(config.getNode("owners"), claim.getOwner().toString());
        }
        if (group == null && claim.isAdminClaim()) {
            group = config.getString("admin-claims");
        }
        if (group == null || group.isEmpty()) {
            group = config.getString("any-claim");
        }
        return group == null || group.isEmpty() ? null : new Match(group, "flan:" + claim.getClaimID(), 0);
    }
}
//#endif
