package de.dosmike.sponge.lockette;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.common.reflect.TypeToken;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class LockSerializer implements TypeSerializer<Map<Location<World>,PluginLock>> {

	/** convert any serializable into a string */
	private String serser(Serializable o) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}
	/** convert serialized string data into a class object */
	private Object serdes(String s) throws IOException, ClassNotFoundException {
		byte[] bdata = Base64.getDecoder().decode(s);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bdata));
		Object o = ois.readObject();
		ois.close();
		return o;
	}
	private Location<World> str2loc (String s) {
		String[] p = s.split("/"); return Sponge.getServer().getWorld(p[0]).get().getLocation(Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]));
	}
	private String loc2str (Location<World> l) {
		return l.getExtent().getName() + "/" + l.getX() + "/" + l.getY() + "/" + l.getZ();
	}
	
	TypeToken<List<UUID>> tokenListUUID = new TypeToken<List<UUID>>() {
		private static final long serialVersionUID = 1217193667806512560L;
	};
	
    @Override
    public Map<Location<World>,PluginLock> deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
    	Map<Location<World>,PluginLock> result = new HashMap<>(); 
        for (Entry<Object, ? extends ConfigurationNode> k : value.getChildrenMap().entrySet()) {
        	Location<World> target = str2loc((String)k.getKey());
        	PluginLock lock = new PluginLock();
        	ConfigurationNode n = k.getValue();
        	lock.active = n.getNode("active").getBoolean();
        	lock.permission = n.getNode("permission").getString();
			lock.permitted = (List<UUID>)n.getNode("permitted").getValue(tokenListUUID);
        	lock.pluginID = n.getNode("pluginID").getString();
        	lock.data = new HashMap<>();
        	n.getNode("data").getChildrenMap().forEach((Object t, ConfigurationNode u)-> {
        		try {
					lock.data.put((String)t, (Serializable)serdes(u.getString()));
				} catch (Exception e) {
					e.printStackTrace();
				}
        	});
        	result.put(target, lock);
        };
        return result;
    }

    @Override
    public void serialize(TypeToken<?> type, Map<Location<World>,PluginLock> obj, ConfigurationNode value) throws ObjectMappingException {
        for (Entry<Location<World>,PluginLock> e : obj.entrySet()) {
        	ConfigurationNode n = value.getNode(loc2str(e.getKey()));
        	n.getNode("active").setValue(e.getValue().active);
        	n.getNode("permission").setValue(e.getValue().permission);
        	n.getNode("permitted").setValue(tokenListUUID, e.getValue().permitted);
        	n.getNode("pluginID").setValue(e.getValue().pluginID);
        	n = n.getNode("data");
        	for (Entry<String, Serializable> d : e.getValue().data.entrySet()) {
	        	try {
	        		n.getNode(d.getKey()).setValue(serser(d.getValue()));
	        	} catch (Exception e1) {
	        		throw new ObjectMappingException("Failed to save custom plugin lock data", e1);
	        	}
        	}
        }
    }
}
