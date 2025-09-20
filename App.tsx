import { StatusBar, StyleSheet, Text, useColorScheme, View } from 'react-native';

function App() {
  const isDarkMode = useColorScheme() === 'dark';

  return (
    <View>
      <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />
      <View style={styles.centerDiv}>
      <Text style={isDarkMode?styles.whiteText:styles.blackText}>Nothing to look for here</Text>
      </View>
    </View>
  );
}


const styles = StyleSheet.create({
  whiteText:{
    color:"white"
  },
  blackText:{
    color:"black"
  },
  centerDiv:{
    display:"flex",
    height:"100%",
    justifyContent:"center",
    alignItems:"center"
  }
});

export default App;
