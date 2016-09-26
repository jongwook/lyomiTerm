//
//  PieConnection.m
//  Pie
//
//

#include <sstream>

#import "PieConnection.h"
#import "PieView.h"

#define BETWEEN(x,a,b) ( (x)>=(a) && (x)<=(b) )

static std::stringstream buffer;
static int bufferlen=0;

BOOL isCP949(unsigned char a, unsigned char b) {
	if(BETWEEN(a,0xA1,0xFE) && BETWEEN(b,0xA1,0xFE))
		return true;
	if(BETWEEN(a,0x81,0xA0) && ( BETWEEN(b,0x41,0x5A) || BETWEEN(b,0x61,0x7A) || BETWEEN(b,0x81,0xFE) ) )
		return true;
	if(BETWEEN(a,0xA1,0xC5) && ( BETWEEN(b,0x41,0x5A) || BETWEEN(b,0x61,0x7A) || BETWEEN(b,0x81,0xA0) ) )
		return true;
	if(a==0xC6 && BETWEEN(b,0x41,0x52))
		return true;
	return false;
	
}

@implementation PieConnection

@synthesize pieView, currentRow, currentCol, encoding;

-(unichar *)screen {
	return (unichar *)screen;
}

-(int *)foreground {
	return (int *)foreground;
}

-(int *)background {
	return (int *)background;
}

-(id) init {
	currentRow=currentCol=0;
	savedRow=savedCol=0;
	currentForeground=-1;
	currentBackground=-2;
	encoding=-2147482590;	// cp949
	for(int i=0;i<TERMINAL_ROWS;i++) {
		for(int j=0;j<TERMINAL_COLS;j++) {
			screen[i][j]=' ';
			foreground[i][j]=-1;
			background[i][j]=-2;
		}
	}
	reversed=NO;
	pos=0;
	stage=0;
	return self;
}

-(BOOL) connectToHost:(NSString *)host {
	return [self connectToHost:host onPort:23];
}

-(BOOL) connectToHost:(NSString *)host onPort:(int)port {
	socket=[[AsyncSocket alloc]initWithDelegate:self userData:0L];
	[socket connectToHost:host onPort:port error:&error];
//	[UIApplication sharedApplication].networkActivityIndicatorVisible = YES;
	return YES;
}

-(void) dealloc {
	[socket release];
	[super dealloc];
}

-(void) onSocketDidDisconnect:(AsyncSocket *)sock {
//	[UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
	[pieView disconnected];
	NSLog(@"Socket Disconnected");
}

-(void) onSocket:(AsyncSocket *)sock willDisconnectWithError:(NSError *)err {
	NSLog(@"Will Disconnect with error : %@",err);
	[pieView disconnected];
}

-(void) onSocket:(AsyncSocket *)sock didConnectToHost:(NSString *)host port:(UInt16)port {
	NSLog(@"Connected to the host!");
	[pieView connected];
	//[UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
	stage=1;
	[self send:"\xff\xfb\x1f"];
	[self send:"\xff\xfb\x20"];
	[self send:"\xff\xfb\x18"];
	[self send:"\xff\xfb\x27"];
	[self send:"\xff\xfd\x01"];
	[self send:"\xff\xfb\x03"];
	[self send:"\xff\xfd\x03"];
	[sock readDataWithTimeout:-1 tag:4321L];
}

-(void) onSocket:(AsyncSocket *)sock didReadData:(NSData *)data withTag:(long)tag { 
//	NSLog(@"Received Data length : %d",data.length);
	buffer.write((const char *)[data bytes],data.length);
	bufferlen+=data.length;
	if(bufferlen<0) {
		for(int i=0;i<bufferlen;i++) {
			int c=buffer.get();
			if(c<0x80) printf("%c",c);
			printf("%02x ",c);
			buffer.write((char *)&c,1);
		}
		printf("\n");
	}
	[self parse];
	[sock readDataWithTimeout:-1 tag:4321L];	
	[pieView setNeedsDisplay:YES];
}

-(void) onSocket:(AsyncSocket *)sock didWriteDataWithTag:(long)tag {
	
}

-(void) parse {
	static char token[64]="";
	
	int cursor=0;

	while(bufferlen>0) {
		if(pos==cursor) {
			token[pos++]=[self getchar];
		}
		cursor++;
		if(token[0]!='\xff') {	// negotiation ended
			stage=2;
//			[UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
		}
		if(token[0]=='\xff') {	// negotiation
			if(pos==cursor) {
				if(bufferlen==0) return;	// required data is not availbe; stop parsing
				token[pos++]=[self getchar];
			}
			cursor++;
			char temp=0;
			switch(token[1]) {	// negotiation 
			case '\xf0':	// suboption end; should not get here
				break;
			case '\xfa':	// suboption start
				while(temp!='\xff') {
					if(pos==cursor) {
						if(bufferlen==0) return;	// stop parsing
						token[pos++]=[self getchar];
					}
					temp=token[cursor];
					cursor++;
				}
				if(pos==cursor) {
					if(bufferlen==0) return;	// stop parsing
					token[pos++]=[self getchar];
				}
				break;
			default:
				if(pos==cursor) {
					if(bufferlen==0) return;
					token[pos++]=[self getchar];
				}
			}
			token[pos]='\0';
			cursor=pos=0;
			[self negotiate:token];
		} else if(token[0]<0) {	// handle cp949 characters
			if(pos==cursor) {
				if(bufferlen==0) return;
				token[pos++]=buffer.peek();
			}
			if(isCP949(token[0],token[1])) {
				token[pos]='\0';
				NSString *str=[NSString stringWithCString:token encoding:encoding];
				unichar c=[str characterAtIndex:0];
				[self drawChar:c];
				[self getchar];
			} else {
				[self drawChar:'?'];
			}
		} else if (token[0]==0x1b) { // escape character
			if(pos==cursor) {
				if(bufferlen==0) return;
				token[pos++]=[self getchar];
			}
			cursor++;
			if (token[1]!='[') {
				token[0]=token[1];
				cursor=0;
				pos=1;
				continue;
			}
			int value[]={0,0,0}, cnt=0;
			while(true) {
				BOOL done=NO;
				if(pos==cursor) {
					if(bufferlen==0) return;
					token[pos++]=[self getchar];
				}
				cursor++;
				switch(token[pos-1]) {
					case '0': case '1': case '2': case '3': case '4':
					case '5': case '6': case '7': case '8': case '9':
						value[cnt]*=10;
						value[cnt]+=token[pos-1]-'0';
						break;
					case ';':
						cnt++;
						break;
					case 'H': // cursor position
						cnt++;
						currentRow=(cnt>=1)?MAX(value[0]-1,0):0;
						currentCol=(cnt>=2)?MAX(value[1]-1,0):0;
						done=YES;
						break;
					case '?': // vt100 mode (no support yet)
						while(token[pos-1]!='h' && token[pos-1]!='l') {
							if(pos==cursor) {
								if(bufferlen==0) return;
								token[pos++]=[self getchar];
							}
							cursor++;
						}
						done=YES;
						break;
					case 'J': case 'j':	// erase screen
						for(int i=0;i<TERMINAL_ROWS;i++) {
							for(int j=0;j<TERMINAL_COLS;j++) { 
								screen[i][j]=' ';
								foreground[i][j]=-1;
								background[i][j]=-2;
							}
						}
						reversed=NO;
						done=YES;
						break;
					case 'M': case 'm': // set graphics mode
						cnt++;
						for(int i=0;i<cnt;i++) {
							if(value[i]==0) {
								currentForeground=-1;
								currentBackground=-2;
								reversed=NO;
							} else if(value[i]==7) {	// reverse video on
								if(reversed==NO) {
									int temp=currentBackground;
									currentBackground=currentForeground;
									currentForeground=temp;
									reversed=YES;
								}
							} else if(value[i]==27) {
								if(reversed==YES) {
									int temp=currentBackground;
									currentBackground=currentForeground;
									currentForeground=temp;
									reversed=NO;
								}
							} else if(value[i]>=30 && value[i]<40) {
								currentForeground=value[i]-30;
							} else if(value[i]>=40 && value[i]<50) {
								currentBackground=value[i]-40;
							}
						}
						done=YES;
						break;
					case 'U': case 'u':	// restore cursor position
						currentCol=savedCol;
						currentRow=savedRow;
						done=YES;
						break;
					case 'S': case 's':	// save cursor position
						savedRow=currentRow;
						savedCol=currentCol;
						done=YES;
						break;
					case 'K':			// erase line
						for(int j=currentCol;j<80;j++) {
							screen[currentRow][j]=' ';
							foreground[currentRow][j]=-1;
							background[currentRow][j]=-2;
						}
						done=YES;
						break;
					default:
						done=YES;
				}
				if(done) {
					token[pos]='\0';
					//NSLog(@"Escape sequence : ^%s",&token[1]);
					break;
				}
			}
		} else if (token[0]=='\n') {
			[self newline];
			//printf("\n");
		} else if (token[0]=='\r') {
			currentCol=0;
		} else if (token[0]=='\t') {
			do {
				[self drawChar:' '];
			} while (currentCol%8!=0 && currentCol<80);
		} else if (token[0]>=0x20 || token[0]=='\b') {
			//printf("%c",token[0]);
			[self drawChar:token[0]];
		}
		cursor=pos=0;
	} 
}

-(char) getchar {
	bufferlen--;
	return buffer.get();
}

-(void) negotiate:(const char *)token {
	NSString *log = [NSString stringWithFormat:@"Negotiating %02x %02x %02x",(UInt8)token[0],(UInt8)token[1],(UInt8)token[2]];
	NSLog(@"%@", log);
	[self.pieView splashStatus: log];

	switch(token[1]) {
		case '\xfa':
			// suboption begin
			switch(token[2]) {
				case '\x18':	// term type
					[self send:"\xff\xfa\x18"];
					[self send:"\x00" length:1];
					[self send:"XTERM"];
					[self send:"\xff\xf0"];
					break;
				case '\x20':	// term speed
					[self send:"\xff\xfa\x20"];
					[self send:"\x00" length:1];
					[self send:"38400,38400"];
					[self send:"\xff\xf0"];
					break;
				case '\x27':	// do new env opt
					[self send:"\xff\xfa\x27"];
					[self send:"\x00" length:1];
					[self send:"\xff\xf0"];
					break;
				default:
					[self send:token length:3];
					[self send:"\x00" length:1];
					[self send:"\xff\xf0"];
			}
			break;
		case '\xfb':
			// will
			switch(token[2]) {
				case '\x01':
					break;
				case '\x03':
					break;
				case '\x05':
					break;
				default:
					[self send:"\xff\xfe"];
					[self send:&token[2] length:1];
			}
			break;
		case '\xfc':
			// won't
			[self send:"\xff\xfe"];
			[self send:&token[2] length:1];
			break;
		case '\xfd':
			// do
			switch(token[2]) {
				case '\x03':
					break;
				case '\x18':	// PieTerminal type
					//[self send:"\xff\xfd\x18"];
					break;
				case '\x1f':	// nego win size
					[self send:"\xff\xfa\x1f"];
					[self send:"\x00\x50\x00\x18" length:4];
					[self send:"\xff\xf0"];
					break;
				case '\x20':	// term speed
					//[self send:"\xff\xfb\x20"];
					break;
				case '\x21':
					break;
				case '\x23':	// x disp loc
					[self send:"\xff\xfc\x23"];
					break;
				case '\x24':	// env opt
					[self send:"\xff\xfc\x24"];
					break;
				case '\x27':	// new env opt
					//[self send:"\xff\xfc\x27"];
					break;
				default:
					[self send:"\xff\xfc"];
					[self send:&token[2] length:1];
			}
			break;
		case '\xfe':
			// don't
			switch(token[2]) {
				case '\x23':	// x disp loc
					[self send:"\xff\xfc\x23"];
					break;
				default:
					[self send:"\xff\xfc"];
					[self send:&token[2] length:1];
			}
			break;
	}	
}

-(void) send:(const char *)token {
	[self send:token length:(int)strlen(token)];
}

-(void) send:(const char *)token length:(int)length {
//	NSLog(@"Sending token length %d",length);
	//for(int i=0;i<length;i++)
	//	printf("%02x ",(UInt8)token[i]);
	//printf("\n");
	NSData *data=[NSData dataWithBytes:token length:length];
	[socket writeData:data withTimeout:-1 tag:1234L];
//	[UIApplication sharedApplication].networkActivityIndicatorVisible = YES;
}

-(void) drawChar:(unichar)c {
	unichar prev=screen[currentRow][currentCol];
	if(c=='\b') {	// backspace
		if(currentCol!=0) {
			currentCol--;
		}
		screen[currentRow][currentCol]=' ';
		foreground[currentRow][currentCol]=-1;
		background[currentRow][currentCol]=-2;
		return;
	}
	if(currentCol>0) {
		unichar left=screen[currentRow][currentCol-1];
		if(left>0x80) {
			screen[currentRow][currentCol-1]='?';
		}
	}
	foreground[currentRow][currentCol]=currentForeground;
	background[currentRow][currentCol]=currentBackground;
	if(prev<0x80) {
		if(c<0x80) {
			screen[currentRow][currentCol]=c;
			currentCol++;
		} else {
			if(currentCol==TERMINAL_COLS-1) {
				screen[currentRow][currentCol]='?';
			} else {
				screen[currentRow][currentCol]=c;
				screen[currentRow][currentCol+1]=' ';
				foreground[currentRow][currentCol+1]=currentForeground;
				background[currentRow][currentCol+1]=currentBackground;
			}
			currentCol+=2;
		}
	} else {
		if(c<0x80) {
			screen[currentRow][currentCol]=c;
			screen[currentRow][currentCol+1]=' ';
			foreground[currentRow][currentCol+1]=currentForeground;
			background[currentRow][currentCol+1]=currentBackground;
			currentCol++;
		} else {
			screen[currentRow][currentCol]=c;
			screen[currentRow][currentCol+1]=' ';
			foreground[currentRow][currentCol+1]=currentForeground;
			background[currentRow][currentCol+1]=currentBackground;
			currentCol+=2;
		}
	}
	
	//NSLog(@"drawChar(%02x) - (%d,%d)",c,currentRow,currentCol);
}

-(void) newline {
	currentCol=0;
	if(currentRow<23) {
		currentRow++;
	} else {
		for(int i=0;i<TERMINAL_ROWS;i++) {
			for(int j=0;j<TERMINAL_COLS;j++) {
				if(i<23) {
					screen[i][j]=screen[i+1][j];
					foreground[i][j]=foreground[i+1][j];
					background[i][j]=background[i+1][j];
				} else {
					screen[i][j]=' ';
					foreground[i][j]=-1;
					background[i][j]=-2;
				}
			}
		}
	}
}

@end
