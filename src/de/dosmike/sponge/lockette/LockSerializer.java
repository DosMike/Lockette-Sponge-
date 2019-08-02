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
import java.util.Map.Entry;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.common.reflect.TypeToken;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class LockSerializer implements TypeSerializer<PluginLock> {

	/** convert any serializable into a string */
	private static String serser(Serializable o) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}
	/** convert serialized string data into a class object */
	private static Object serdes(String s) throws IOException, ClassNotFoundException {
		byte[] bdata = Base64.getDecoder().decode(s);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bdata));
		Object o = ois.readObject();
		ois.close();
		return o;
	}
	static Location<World> str2loc (String s) {
		String[] p = s.split("/"); return Sponge.getServer().getWorld(p[0]).get().getLocation(Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]));
	}
	static String loc2str (Location<World> l) {
		return l.getExtent().getName() + "/" + l.getX() + "/" + l.getY() + "/" + l.getZ();
	}
	
	TypeToken<List<UUID>> tokenListUUID = new TypeToken<List<UUID>>() {
		private static final long serialVersionUID = 1217193667806512560L;
	};
	
    @Override
    public PluginLock deserialize(TypeToken<?> type, ConfigurationNode cfg) throws ObjectMappingException {
		PluginLock lock = new PluginLock();
		String tmp = cfg.getNode("owner").getString();
		lock.owner = tmp==null?null:UUID.fromString(tmp);
    	lock.active = cfg.getNode("active").getBoolean();
    	lock.permission = cfg.getNode("permission").getString();
		lock.permitted = (List<UUID>)cfg.getNode("permitted").getValue(tokenListUUID);
    	lock.pluginID = cfg.getNode("pluginID").getString();
    	lock.data = new HashMap<>();
    	cfg.getNode("data").getChildrenMap().forEach((Object t, ConfigurationNode u)-> {
    		try {
				lock.data.put((String)t, (Serializable)serdes(u.getString()));
			} catch (Exception e) {
				e.printStackTrace();
			}
    	});
    	return lock;
    }

    @Override
    public void serialize(TypeToken<?> type, PluginLock obj, ConfigurationNode cfg) throws ObjectMappingException {
    		cfg.getNode("active").setValue(obj.active);
    		cfg.getNode("owner").setValue(obj.owner.toString());
        	cfg.getNode("permission").setValue(obj.permission);
        	cfg.getNode("permitted").setValue(tokenListUUID, obj.permitted);
        	cfg.getNode("pluginID").setValue(obj.pluginID);
        	cfg = cfg.getNode("data");
        	for (Entry<String, Serializable> d : obj.data.entrySet()) {
	        	try {
	        		cfg.getNode(d.getKey()).setValue(serser(d.getValue()));
	        	} catch (Exception e1) {
	        		throw new ObjectMappingException("Failed to save custom plugin lock data", e1);
	        	}
        	}
    }
}
