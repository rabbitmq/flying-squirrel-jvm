package com.rabbitmq.socks.api;

/**
 * 
 * @author tfox
 *
 */
public enum ChannelType
{
	PUB, SUB, PUSH, PULL, REQ, REP;
	
	public String toString()
	{
		if (this == ChannelType.PUB)
		{
			return "pub";
		}
		else if (this == ChannelType.SUB)
		{
			return "sub";
		}
		else if (this == ChannelType.PULL)
		{
			return "pull";
		}
		else if (this == ChannelType.PUSH)
		{
			return "push";
		}
		else if (this == ChannelType.REQ)
		{
			return "req";
		}
		else if (this == ChannelType.REP)
		{
			return "rep";
		}
		else
		{
			throw new IllegalStateException("Invalid ChannelType");
		}
	}
	
	public static ChannelType fromString(final String str)
	{
		if ("pub".equals(str))
		{
			return ChannelType.PUB;
		}
		else if ("sub".equals(str))
		{
			return ChannelType.SUB;
		}
		else if ("pull".equals(str))
		{
			return ChannelType.PULL;
		}
		else if ("push".equals(str))
		{
			return ChannelType.PUSH;
		}
		else if ("req".equals(str))
		{
			return ChannelType.REQ;
		}
		else if ("rep".equals(str))
		{
			return ChannelType.REP;
		}
		else
		{
			throw new IllegalArgumentException("Invalid string rep: " + str);
		}
	}
}

