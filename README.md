# Waves Reward Distributer

## How to use
### Download archive for your OS or fat JAR from [releases](https://github.com/suadzh/WavesDistributer/releases/)
### Windows or Linux binary
1. Extract archive
2. Open PowerShell or CMD:
```
# Set interval to 0 if the script shall only trigger once

# Windows
.\distributer.exe -p [Base58PrivateKey] -b [BeneficiaryAddress] -i [InvokeInterval(Hours)]

# Linux
./distributer -p [Base58PrivateKey] -b [BeneficiaryAddress] -i [InvokeInterval(Hours)]
```
### Cross-Platform / JAR
```
# Set interval to 0 if the script shall be triggered once only

# Install JDK
sudo apt install default-jdk
java -jar ./distributer-assembly-0.x.x.jar -p [Base58PrivateKey] -b [BeneficiaryAddress] -i [InvokeInterval(Hours)]
```