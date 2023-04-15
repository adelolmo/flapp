package flands.resources;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class Resources {
	public static ResourceBundle GuiText()
	{
		return ResourceBundle.getBundle("flands.resources.GuiTextResource");
	}
	
	public static String GuiText(String key)
	{
		try
		{
			return GuiText().getString(key);
		}
		catch (Exception e)
		{
			System.err.println(GuiTextFormat("GuiResourceLoadException", e));
			return key;
		}
	}
	
	public static String GuiTextFormat(String key, Object... arguments)
	{
		try
		{
			String value = GuiText().getString(key);
			return MessageFormat.format(value, arguments);
		}
		catch (Exception e)
		{
			System.err.println(GuiTextFormat("GuiResourceLoadException", e));
			return key;
		}
	}
}
