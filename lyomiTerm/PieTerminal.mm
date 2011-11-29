#include "PieTerminal.h"

//static const char* controlChars[]={"NUL","SOH","STX","ETX","EOT","ENQ","ACK","BEL",
//	"BS" ,"HT" ,"LF" ,"VT" ,"FF" ,"CR" ,"SO" ,"SI" ,
//	"DLE","DC1","DC2","DC3","DC4","NAK","SYN","ETB",
//	"CAN","EM" ,"SUB","ESC","FS" ,"GS" ,"RS" ,"US" };

#define BETWEEN(x,a,b) ( (x)>=(a) && (x)<=(b) )

PieTerminal::PieTerminal(void) {
	sock=[[AsyncSocket alloc] init];
	bufferlen=0;
	terminated=false;
	bold=false;
	underscore=false;
	foreground=-1;
	background=-1;
	cursorx=cursory=0;
	savex=savey=0;
	for(int i=0;i<24;i++) {
		for(int j=0;j<80;j++) {
			screen[i][j].character=0;
			cscreen[i][j]=' ';
		}
	}
}


PieTerminal::~PieTerminal(void) {
	if(sock) {
		[sock release];
	}
}

void PieTerminal::connect(const char *host, int port) {

	printf("connecting to %s...\n",host);
//	if(socket_connect(sock,host,port)==-1) 
	if(![sock connectToHost:[NSString stringWithUTF8String:host] onPort:port error:nil])
		return;

	// start negotiation
	send("\xff\xfb\x1f");
	send("\xff\xfb\x20");
	send("\xff\xfb\x18");
	send("\xff\xfb\x27");
	send("\xff\xfd\x01");
	send("\xff\xfb\x03");
	send("\xff\xfd\x03");
}

void PieTerminal::close() {
	[sock disconnect];
}

void PieTerminal::negotiate(const char *token) {
	switch(token[1]) {
	case '\xfa':
		// suboption begin
		switch(token[2]) {
		case '\x18':	// term type
			send("\xff\xfa\x18");
			send("\x00",1);
			send("XTERM");
			send("\xff\xf0");
			break;
		case '\x20':	// term speed
			send("\xff\xfa\x20");
			send("\x00",1);
			send("38400,38400");
			send("\xff\xf0");
			break;
		case '\x27':	// do new env opt
			send("\xff\xfa\x27");
			send("\x00",1);
			send("\xff\xf0");
			break;
		default:
			send(token,3);
			send("\x00",1);
			send("\xff\xf0");
		}
		break;
	case '\xfb':
		// will
		break;
	case '\xfc':
		// won't
		break;
	case '\xfd':
		// do
		switch(token[2]) {
		case '\x18':	// PieTerminal type
			send("\xff\xfd\x18");
			break;
		case '\x1f':	// nego win size
			send("\xff\xfa\x1f");
			send("\x00\x50\x00\x18",4);
			send("\xff\xf0");
			break;
		case '\x20':	// term speed
			send("\xff\xfb\x18");
			break;
		case '\x23':	// x disp loc
			send("\xff\xfc\x23");
			break;
		case '\x24':	// env opt
			send("\xff\xfc\x24");
			break;
		case '\x27':	// new env opt
			send("\xff\xfc\x23");
			break;
		default:
			send("\xff\xfc");
			send(&token[2],1);
		}
		break;
	case '\xfe':
		// don't
		switch(token[2]) {
		case '\x23':	// x disp loc
			send("\xff\xfc\x23");
			break;
		default:
			send("\xff\xfc");
			send(&token[2],1);
		}
		break;
	}
}

void PieTerminal::update() {
	static char buf[512];
	while(false) {
		int len=0;//socket_receive(sock,buf,512);
		if(len==-1) {
			bufferlen=-1;
			return;
		}
		bufferlen+=len;
		buffer.write(buf,len);
	}
}

void PieTerminal::idle() {
	while(!terminated) {
		parse();
		draw();
	}
}

void PieTerminal::draw() {
	system("cls");
	for(int i=0;i<24;i++) {
		for(int j=0;j<80;j++) {
			char c=cscreen[i][j];
			printf("%c",(c=='\0')?' ':c);
		}
	}
}

void PieTerminal::parse() {
	static char result[256];
	static int pos=0;
	char next=getchar();

	if(next=='\xff') {	// negotiation
		result[pos++]=next;
		char nego=getchar();
		result[pos++]=nego;
		char option=0;
		switch(nego) {
		case '\xf0':
			// should not reach here
			break;
		case '\xfa':	// suboption start
			while(option!='\xff') result[pos++]=option=getchar();
			result[pos++]=getchar();	// this should be f0
			break;
		default:
			option=getchar();
			result[pos++]=option;
		}
		result[pos]='\0';
		pos=0;
		negotiate(result);
		return;
	}

	if(next==EOF) {
		return;
	}

	// handle mbcs characters
	if(next<0) {
		char second=peek();
		bool cp949=false;
		if(BETWEEN(next,'\xa1','\xfe') && BETWEEN(second,'\xa1','\xfe')) cp949=true;
		if(BETWEEN(next,'\x81','\xc5') && ( BETWEEN(second,'\x41','\x5a') || BETWEEN(second,'\x61','\x7a') || BETWEEN(second,'\x81','\xfe') )) cp949=true;
		if(next=='\xc6' && BETWEEN(second,'\x41','\x52') ) cp949=true;
		
		if(cp949) {	
			getchar();
			//drawchar(cp949table[(unsigned char)next*256+(unsigned char)second]);
			drawchar((unsigned char)next*256+(unsigned char)second);
		}
		next='?';
	}
		
	if(next=='\n') {
		newline();
		return;
	}

	if(next=='\r' || next=='\0') {
		return;
	}

	if(next==0x1b) {	// escape character
		char second=peek();
		if(second!='[') return;
		int value[]={0,0,0}, cnt=0;
		while(true) {
			second=getchar();
			switch(second) {
			case '0': case '1': case '2': case '3': case '4':
			case '5': case '6': case '7': case '8': case '9':
				value[cnt]*=10;
				value[cnt]+=second-'0';
				break;
			case ';':
				cnt++;
				break;
			case 'H': case 'h':	// cursor position
				cnt++;
				cursory=(cnt>=1)?value[0]:0;
				cursorx=(cnt>=2)?value[1]:0;
				return;
			case 'J': case 'j':	// erase screen
				for(int i=0;i<24;i++)
					for(int j=0;j<80;j++) 
						screen[i][j].character=' ';
				return;
			case 'M': case 'm': // set graphics mode
				cnt++;
				for(int i=0;i<cnt;i++) {
					if(value[i]==0) {
						foreground=-1;
						background=-1;
						bold=false;
						underscore=false;
					} else if(value[i]==1) {
						bold=true;
					} else if(value[i]==4) {
						underscore=true;
					} else if(value[i]>=30 && value[i]<40) {
						foreground=value[i]-30;
					} else if(value[i]>=40 && value[i]<50) {
						background=value[i]-40;
					}
				}
				return;
			case 'U': case 'u':	// restore cursor position
				cursorx=savex;
				cursory=savey;
				return;
			case 'S': case 's':	// save cursor position
				savex=cursorx;
				savey=cursory;
				return;
			}
		}
	}
	
	drawchar(next);
}

void PieTerminal::drawchar(int c) {
	termchar &s=screen[cursory][cursorx];
	if(c<0x80) {	// normal ascii string
		s.bold=bold;
		s.underscore=underscore;
		s.foreground=foreground;
		s.background=background;
		s.character=c;
		cscreen[cursory][cursorx]=(c==0)?' ':c;
		cursorx=(cursorx>=79)?79:(cursorx+1);
	} else {
		// check its left
		if(cursorx>0) {
			termchar &left=screen[cursory][cursorx-1];
			if(left.character>0x80) {
				left.character='?';
			}
		}
		// check its right
		if(cursorx<78) {
			termchar &right=screen[cursory][cursorx+2];
			if(right.character>0x80) {
				right.character='?';
			}
		}
		s.bold=bold;
		s.underscore=underscore;
		s.foreground=foreground;
		s.background=background;
		s.character=(cursorx==79)?'?':c;

		// clear the second slot
		if(cursorx<79) {
			termchar &right=screen[cursory][cursorx+2];
			right.character=0;
		}
		cscreen[cursory][cursorx]=(c/256);
		cscreen[cursory][cursorx+1]=(c%256);
		cursorx=(cursorx>=79)?79:(cursorx+2);
	}
}

void PieTerminal::newline() {
	cursorx=0;
	if(cursory<23) {
		cursory++;
	} else {
		for(int i=0;i<24;i++) {
			for(int j=0;j<80;j++) {
				if(i<23) screen[i][j]=screen[i+1][j];
				else screen[i][j].character=0;
			}
		}
	}
}

char PieTerminal::getchar() {
	while(bufferlen<=0) update();
	char result;
	buffer.get(result);
	bufferlen--;
	return result;
}

char PieTerminal::peek() {
	while(bufferlen<=0) update();
	return buffer.peek();
}

void PieTerminal::send(const char * data, int len) {
	if(len==-1) len=strlen(data);
	//socket_send(sock,data,len);
}
