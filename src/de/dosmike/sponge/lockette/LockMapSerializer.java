package de.dosmike.sponge.lockette;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.common.reflect.TypeToken;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class LockMapSerializer implements TypeSerializer<Map<Location<World>,PluginLock>> {

	static Location<World> str2loc (String s) {
		String[] p = s.split("/"); return Sponge.getServer().getWorld(p[0]).get().getLocation(Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]));
	}
	static String loc2str (Location<World> l) {
		return l.getExtent().getName() + "/" + l.getX() + "/" + l.getY() + "/" + l.getZ();
	}
	
    @Override
    public Map<Location<World>,PluginLock> deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
    	Map<Location<World>,PluginLock> result = new HashMap<>(); 
        for (Entry<Object, ? extends ConfigurationNode> k : value.getChildrenMap().entrySet()) {
        	Location<World> target = str2loc((String)k.getKey());

        	PluginLock lock;
			try {
				lock = getPluginLock(k.getValue());
			} catch (ClassNotFoundException e) {
				throw new ObjectMappingException("Some plugins that created locks seem to be missing");
			}
        	
        	result.put(target, lock);
        };
        return result;
    }

    @Override
    public void serialize(TypeToken<?> type, Map<Location<World>,PluginLock> obj, ConfigurationNode value) throws ObjectMappingException {
        for (Entry<Location<World>,PluginLock> e : obj.entrySet()) {
        	setPluginLock(
        			value.getNode(loc2str(e.getKey())),
        			e.getValue()
        		);
        }
    }
    
    public <X extends PluginLock> void setPluginLock(ConfigurationNode n, X x) throws ObjectMappingException {
    	TypeToken<X> type = (TypeToken<X>) TypeToken.of(x.getClass());
    	n.getNode("instance").setValue(type, x);
    	n.getNode("class").setValue(x.getClass().getName());
    }
    public <X extends PluginLock> X getPluginLock(ConfigurationNode n) throws ClassNotFoundException, ObjectMappingException {
    	Class<X> type = (Class<X>) Class.forName(n.getNode("class").getString());
    	TypeToken<X> tt = (TypeToken<X>) TypeToken.of(type);
    	return n.getNode("instance").getValue(tt);
    }
}
