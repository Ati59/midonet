#!/bin/sh

# This scripts creates two network namespaces with a network interface
# each that's mapped to a network interface in the host, which can then
# be plugged to the datapath by binding it to midonet's virtual topology.

# The namespaces will be called 'left' and 'right'
# The interfaces prepared for binding to Midonet's vports are 'leftdp' and
# 'rightdp' respectively.
# The interfaces inside the namespaces are 'leftns' and 'rightns'

# 'leftns' and 'rightns' are given addresses 10.25.25.1/24 and 10.25.25.2/24
# respectively, so this script expects that they will be plugged to the same
# virtual bridge inside midonet.

# Once the script has been run, the virtual topology is setup as explained above
# and midolman is up and running, traffic can be sent from one namespace to the
# other, and arbitrary commands can be run in side them. Two examples:

# Ping right from left:
#   ip netns exec left ping 10.25.25.2
# tcpdump on the right side:
#   ip netns exec right tcpdump -i rightns

ip netns add left
ip link add name leftdp type veth peer name leftns
ip link set leftdp up
ip link set leftns netns left
ip netns exec left ip link set leftns up
ip netns exec left ip address add 10.25.25.1/24 dev leftns
ip netns exec left ifconfig lo up


ip netns add right
ip link add name rightdp type veth peer name rightns
ip link set rightdp up
ip link set rightns netns right
ip netns exec right ip link set rightns up
ip netns exec right ip address add 10.25.25.2/24 dev rightns
ip netns exec right ifconfig lo up
