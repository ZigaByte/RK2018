import socket
import struct
import sys
import threading


PORT = 8888
HEADER_LENGTH = 2
SERVER_IP =  "localhost" #"212.235.182.78" #"127.0.0.1" "193.2.177.214" #"2001:1470:fffd:c000:97:28a3:5252:f0d9"  #


def receive_fixed_length_msg(sock, msglen):
	message = b''
	while len(message) < msglen:
		chunk = sock.recv(msglen - len(message)) # preberi nekaj bajtov
		if chunk == b'':
			raise RuntimeError("socket connection broken")
		message = message + chunk # pripni prebrane bajte sporocilu

	return message

def receive_message(sock):
	header = receive_fixed_length_msg(sock, HEADER_LENGTH) # preberi glavo sporocila (v prvih 2 bytih je dolzina sporocila)
	message_length = struct.unpack("!H", header)[0] # pretvori dolzino sporocila v int

	message = None
	if message_length > 0: # ce je vse OK
		message = receive_fixed_length_msg(sock, message_length) # preberi sporocilo
		message = message.decode("utf-8")

	return message

def send_message(sock, message):
	encoded_message = message.encode("utf-8") # pretvori sporocilo v niz bajtov, uporabi UTF-8 kodno tabelo

	# ustvari glavo v prvih 2 bytih je dolzina sporocila (HEADER_LENGTH)
	# metoda pack "!H" : !=network byte order, H=unsigned short
	header = struct.pack("!H", len(encoded_message))

	message = header + encoded_message # najprj posljemo dolzino sporocilo, slee nato sporocilo samo
	sock.sendall(message);

# message_receiver funkcija tece v loceni niti
def message_receiver():
	while True:
		msg_received = receive_message(sock)
		if len(msg_received) > 0: # ce obstaja sporocilo
			print("[RKchat] " + msg_received) # izpisi



# povezi se na streznik
print("[system] connecting to chat server ...")
sock = socket.socket(socket.AF_INET6, socket.SOCK_STREAM) # for ipv4 socket.AF_INET
sock.connect((SERVER_IP, PORT))
print("[system] connected!")

# zazeni message_receiver funkcijo v loceni niti
thread = threading.Thread(target=message_receiver)
thread.daemon = True
thread.start()

# pocakaj da uporabnik nekaj natipka in poslji na streznik
while True:
	try:
		msg_send = input("")
		send_message(sock, msg_send)
	except KeyboardInterrupt:
		sys.exit()
