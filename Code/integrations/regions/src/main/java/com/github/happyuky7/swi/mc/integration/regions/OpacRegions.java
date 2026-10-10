//#if MC >= 1.21
package com.github.happyuky7.swi.mc.integration.regions;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.mc.compat.Mc;
import com.github.happyuky7.swi.mc.region.ModRegions;
import com.github.happyuky7.swi.mc.region.RegionSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.UUID;

/**
 * Open Parties and Claims: chunk claims owned by a player, who may lead or belong to a party.
 *
 * <pre>
 * regions:
 *   opac:
 *     owners: { "&lt;owner uuid&gt;": base }            # chunks claimed by a player
 *     parties: { "&lt;party uuid or name&gt;": team }   # chunks claimed by any party member
 *     any-claim: claims                             # any other claimed chunk
 * </pre>
 */
public final class OpacRegions implements RegionSource {

    @Override
    public String id() {
        return "opac";
    }

    @Override
    public Match match(ServerPlayer player, DataNode config) {
        ServerLevel level = Mc.level(player);
        OpenPACServerAPI api = OpenPACServerAPI.get(level.getServer());
        //#if MC >= 1.21.11
        IPlayerChunkClaimAPI claim = api.getServerClaimsManager().get(level.dimension().identifier(), player.blockPosition());
        //#else
        //$$ IPlayerChunkClaimAPI claim = api.getServerClaimsManager().get(level.dimension().location(), player.blockPosition());
        //#endif
        if (claim == null || claim.getPlayerId() == null) {
            return null;
        }
        UUID owner = claim.getPlayerId();
        String group = ModRegions.lookup(config.getNode("owners"), owner.toString());
        if (group == null) {
            IServerPartyAPI party = api.getPartyManager().getPartyByMember(owner);
            if (party != null) {
                group = ModRegions.lookup(config.getNode("parties"), party.getId().toString(), party.getDefaultName());
            }
        }
        if (group == null || group.isEmpty()) {
            group = config.getString("any-claim");
        }
        return group == null || group.isEmpty() ? null : new Match(group, "opac:" + owner, 0);
    }
}
//#endif
